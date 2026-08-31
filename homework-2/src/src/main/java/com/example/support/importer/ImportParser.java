package com.example.support.importer;

import java.io.InputStream;
import java.util.List;

public interface ImportParser {
    List<ParseResult> parse(InputStream inputStream);
}
