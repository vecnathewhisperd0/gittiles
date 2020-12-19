// Copyright 2020 Google LLC. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles;

import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.gitiles.FormatType.HTML;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static org.eclipse.jgit.http.server.ServletUtils.ATTRIBUTE_REPOSITORY;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.http.server.ServletUtils;
import org.eclipse.jgit.lib.Repository;

/**
 * Filter to replace the URL string that contains a branch name to a new branch name. The updated
 * branch mapping is provided by {@code BranchRedirectFilter#getRedirectBranch} method If it updates
 * the branch it then returns the new URL with updated branch name as redirect.
 *
 * <p>This implementation does not provide a branch redirect mapping. Hence including this filter
 * as-is would be a no-op. To make this effective {@code BranchRedirectFilter#getRedirectBranch}
 * needs to be overridden that provides a mapping to the requested repo/branch.
 */
public class BranchRedirectFilter extends AbstractHttpFilter {
  /**
   * Provides an extendable interface that can be used to provide implementation for determining
   * redirect branch
   *
   * @param repo Repository
   * @param sourceBranch full branch name eg. refs/heads/master
   * @param formatType format type of the request
   * @return Returns the branch that should be redirected to on a given repo. {@code
   *     Optional.empty()} means no redirect.
   */
  protected Optional<String> getRedirectBranch(
      Repository repo, String sourceBranch, FormatType formatType) {
    return Optional.empty();
  }

  private Optional<String> rewriteRevision(Repository repo, Revision rev, FormatType format) {
    if (Revision.isNull(rev)) {
      return Optional.empty();
    }
    return getRedirectBranch(repo, rev.getName(), format);
  }

  private static Revision rewriteRevision(Revision revision, Optional<String> targetBranch) {
    if (!targetBranch.isPresent()) {
      return revision;
    }

    return new Revision(
        targetBranch.get(),
        revision.getId(),
        revision.getType(),
        revision.getPeeledId(),
        revision.getPeeledType());
  }

  @Override
  public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    if (!hasRepository(req)) {
      chain.doFilter(req, res);
      return;
    }

    GitilesView view = ViewFilter.getView(req);
    Repository repo = ServletUtils.getRepository(req);
    FormatType formatType = getFormat(view);

    Optional<String> rewrittenRevision = rewriteRevision(repo, view.getRevision(), formatType);
    Optional<String> rewrittenOldRevision =
        rewriteRevision(repo, view.getOldRevision(), formatType);

    if (!rewrittenRevision.isPresent() && !rewrittenOldRevision.isPresent()) {
      chain.doFilter(req, res);
      return;
    }

    Revision rev = rewriteRevision(view.getRevision(), rewrittenRevision);
    Revision oldRev = rewriteRevision(view.getOldRevision(), rewrittenOldRevision);

    String url = view.toBuilder().setRevision(rev).setOldRevision(oldRev).toUrl();
    res.setStatus(SC_MOVED_PERMANENTLY);
    res.setHeader(LOCATION, url);
  }

  private static boolean hasRepository(HttpServletRequest req) {
    return req.getAttribute(ATTRIBUTE_REPOSITORY) != null;
  }

  private static FormatType getFormat(GitilesView view) {
    Optional<String> format = view.getParameters().get("format").stream().findAny();
    if (!format.isPresent()) {
      return HTML;
    }
    for (FormatType type : FormatType.values()) {
      if (type.name().equals(format.get())) {
        return type;
      }
    }
    return HTML;
  }
}
