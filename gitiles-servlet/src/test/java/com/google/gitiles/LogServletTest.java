// Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;

/** Tests for {@link LogServlet}. */
@RunWith(JUnit4.class)
public class LogServletTest extends ServletTest {
  @Test
  public void emptyLog() throws Exception {
    RevCommit commit = repo.branch("refs/heads/master").commit().create();
    repo.getRepository().updateRef("HEAD").link("refs/heads/master");

    JsonElement result = buildJson("/repo/+log");
    JsonArray log = result.getAsJsonObject().get("log").getAsJsonArray();

    assertThat(log).hasSize(1);
    CommitJsonData.Commit jsonCommit = new Gson().fromJson(log.get(0), CommitJsonData.Commit.class);
    assertThat(jsonCommit.commit).isEqualTo(commit.name());
    assertThat(jsonCommit.tree).isNotNull();
    assertThat(jsonCommit.parents).isEmpty();
    assertThat(jsonCommit.author).isNotNull();
    assertThat(jsonCommit.committer).isNotNull();
    assertThat(jsonCommit.message).isNotNull();
    assertThat(jsonCommit.treeDiff).isNull();
  }
}
