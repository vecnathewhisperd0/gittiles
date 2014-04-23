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

import static org.eclipse.jgit.lib.Constants.OBJ_COMMIT;

import com.google.common.collect.Maps;
import com.google.gitiles.codemirror.Modes;

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;

import java.io.IOException;
import java.util.Map;

/** Soy data converter for git blobs. */
public class BlobSoyData {
  /**
   * Maximum number of bytes to load from a supposed text file for display.
   * Files larger than this will be displayed as binary files, even if the
   * contents was text. For example really big XML files may be above this limit
   * and will get displayed as binary.
   */
  private static final int MAX_FILE_SIZE = 10 << 20;

  private final String staticPrefix;
  private final GitilesView view;
  private final RevWalk walk;

  public BlobSoyData(String staticPrefix, RevWalk walk, GitilesView view) {
    this.staticPrefix = staticPrefix;
    this.view = view;
    this.walk = walk;
  }

  public Map<String, Object> toSoyData(ObjectId blobId)
      throws MissingObjectException, IOException {
    return toSoyData(null, blobId);
  }

  public Map<String, Object> toSoyData(String path, ObjectId blobId)
      throws MissingObjectException, IOException {
    Map<String, Object> data = Maps.newHashMapWithExpectedSize(4);
    data.put("sha", ObjectId.toString(blobId));

    ObjectLoader loader = walk.getObjectReader().open(blobId, Constants.OBJ_BLOB);
    byte[] raw;
    String content;
    try {
      raw = loader.getCachedBytes(MAX_FILE_SIZE);
      content = !RawText.isBinary(raw) ? RawParseUtils.decode(raw) : null;
    } catch (LargeObjectException.OutOfMemory e) {
      throw e;
    } catch (LargeObjectException e) {
      raw = null;
      content = null;
    }

    data.put("data", content);
    if (content != null) {
      data.put("codemirror", Modes.toSoyData(staticPrefix, path, raw));
    } else {
      data.put("size", Long.toString(loader.getSize()));
    }
    if (path != null && view.getRevision().getPeeledType() == OBJ_COMMIT) {
      data.put("logUrl", GitilesView.log().copyFrom(view).toUrl());
      data.put("blameUrl", GitilesView.blame().copyFrom(view).toUrl());
    }
    return data;
  }
}
