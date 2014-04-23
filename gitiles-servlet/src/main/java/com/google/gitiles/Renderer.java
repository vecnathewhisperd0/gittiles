// Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.template.soy.tofu.SoyTofu;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/** Renderer for Soy templates used by Gitiles. */
public abstract class Renderer {
  private static final List<String> SOY_FILENAMES = ImmutableList.of(
      "BlameDetail.soy",
      "Common.soy",
      "DiffDetail.soy",
      "HostIndex.soy",
      "LogDetail.soy",
      "ObjectDetail.soy",
      "PathDetail.soy",
      "RefList.soy",
      "RevisionDetail.soy",
      "RepositoryIndex.soy");

  public static final Map<String, String> STATIC_URL_GLOBALS = ImmutableMap.of(
      "gitiles.CM_CSS_URL", "codemirror/cm4.css",
      "gitiles.CM_JS_URL", "codemirror/cm4.js",
      "gitiles.CSS_URL", "gitiles.css");

  protected static class FileUrlMapper implements Function<String, URL> {
    private final String prefix;

    protected FileUrlMapper() {
      this("");
    }

    protected FileUrlMapper(String prefix) {
      this.prefix = checkNotNull(prefix, "prefix");
    }

    @Override
    public URL apply(String filename) {
      if (filename == null) {
        return null;
      }
      try {
        return new File(prefix + filename).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  protected ImmutableList<URL> templates;
  protected ImmutableMap<String, String> globals;
  private String staticPrefix;

  protected Renderer(Function<String, URL> resourceMapper, Map<String, String> globals,
      String staticPrefix, Iterable<URL> customTemplates, String siteTitle) {
    this.staticPrefix =checkNotNull(staticPrefix, "staticPrefix");
    Iterable<URL> allTemplates = FluentIterable.from(SOY_FILENAMES).transform(resourceMapper);
    templates = ImmutableList.copyOf(Iterables.concat(allTemplates, customTemplates));

    Map<String, String> allGlobals = Maps.newHashMap();
    for (Map.Entry<String, String> e : STATIC_URL_GLOBALS.entrySet()) {
      allGlobals.put(e.getKey(), staticPrefix + e.getValue());
    }
    allGlobals.put("gitiles.SITE_TITLE", siteTitle);
    allGlobals.putAll(globals);
    this.globals = ImmutableMap.copyOf(allGlobals);
  }

  public String getStaticPrefix() {
    return staticPrefix;
  }

  public void render(HttpServletResponse res, String templateName) throws IOException {
    render(res, templateName, ImmutableMap.<String, Object> of());
  }

  public void render(HttpServletResponse res, String templateName, Map<String, ?> soyData)
      throws IOException {
    res.setContentType("text/html");
    res.setCharacterEncoding("UTF-8");
    byte[] data = newRenderer(templateName).setData(soyData).render().getBytes(Charsets.UTF_8);
    res.setContentLength(data.length);
    res.getOutputStream().write(data);
  }

  SoyTofu.Renderer newRenderer(String templateName) {
    return getTofu().newRenderer(templateName);
  }

  protected abstract SoyTofu getTofu();
}
