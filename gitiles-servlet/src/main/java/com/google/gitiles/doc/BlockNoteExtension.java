// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles.doc;

import com.google.common.collect.ImmutableSet;

import org.commonmark.Extension;
import org.commonmark.node.Block;
import org.commonmark.parser.Parser;
import org.commonmark.parser.Parser.ParserExtension;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.AbstractBlockParserFactory;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.block.BlockStart;
import org.commonmark.parser.block.MatchedBlockParser;
import org.commonmark.parser.block.ParserState;

/**
 * CommonMark extension for block notes.
 * <pre>
 * *** note
 * This is a note.
 * ***
 * </pre>
 */
public class BlockNoteExtension implements ParserExtension {
  private static final ImmutableSet<String> VALID_STYLES =
      ImmutableSet.of("note", "aside", "promo");

  public static Extension create() {
    return new BlockNoteExtension();
  }

  private BlockNoteExtension() {}

  @Override
  public void extend(Parser.Builder builder) {
    builder.customBlockParserFactory(new DivParserFactory());
  }

  private static class DivParser extends AbstractBlockParser {
    private BlockNote block;
    private boolean done;

    DivParser(String style) {
      block = new BlockNote();
      block.setClassName(style);
    }

    @Override
    public Block getBlock() {
      return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
      if (done) {
        return BlockContinue.none();
      }
      if (state.getIndent() == 0) {
        int s = state.getNextNonSpaceIndex();
        CharSequence line = state.getLine();
        if ("***".contentEquals(line.subSequence(s, line.length()))) {
          done = true;
          return BlockContinue.atIndex(line.length());
        }
      }
      return BlockContinue.atIndex(state.getIndex());
    }

    @Override
    public boolean isContainer() {
      return true;
    }

    @Override
    public boolean canContain(Block block) {
      return true;
    }
  }

  private static class DivParserFactory extends AbstractBlockParserFactory {
    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matched) {
      if (state.getIndent() > 0) {
        return BlockStart.none();
      }

      int s = state.getNextNonSpaceIndex();
      CharSequence line = state.getLine();
      CharSequence text = line.subSequence(s, line.length());
      if (text.length() < 4 || !"*** ".contentEquals(text.subSequence(0, 4))) {
        return BlockStart.none();
      }

      String style = text.subSequence(4, line.length()).toString().trim();
      if (!VALID_STYLES.contains(style)) {
        return BlockStart.none();
      }

      return BlockStart.of(new DivParser(style)).atIndex(line.length());
    }
  }
}
