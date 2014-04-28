// Copyright (C) 2014 Google Inc. All Rights Reserved.
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

import com.google.common.collect.Lists;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.SystemReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/** Date formatter similar in spirit to JGit's {@code GitDateFormatter}. */
public class DateFormatterBuilder {
  public static enum Format {
    // Format strings should match org.eclipse.jgit.util.GitDateFormatter except
    // for the timezone suffix.
    DEFAULT("EEE MMM dd HH:mm:ss yyyy"),
    ISO("yyyy-MM-dd HH:mm:ss");

    private final String fmt;

    private Format(String fmt) {
      this.fmt = fmt;
    }
  }

  public class DateFormatter {
    private final Format format;

    private DateFormatter(Format format) {
      this.format = format;
    }

    public String format(PersonIdent ident) {
      DateFormat df = getDateFormat(format);
      TimeZone tz = ident.getTimeZone();
      if (tz == null) {
        tz = SystemReader.getInstance().getTimeZone();
      }
      df.setTimeZone(tz);
      return df.format(ident.getWhen());
    }
  }

  private final ThreadLocal<List<DateFormat>> dfs;

  DateFormatterBuilder() {
    this.dfs = new ThreadLocal<List<DateFormat>>();
  }

  public DateFormatter create(Format format) {
    return new DateFormatter(format);
  }

  private DateFormat getDateFormat(Format format) {
    List<DateFormat> result = dfs.get();
    if (result == null) {
      int n = Format.values().length;
      result = Lists.newArrayListWithCapacity(n);
      for (int i = 0; i < n; i++) {
        result.add(null);
      }
      dfs.set(result);
    }
    DateFormat df = result.get(format.ordinal());
    if (df == null) {
      df = new SimpleDateFormat(format.fmt + " Z");
      result.set(format.ordinal(), df);
    }
    return result.get(format.ordinal());
  }
}
