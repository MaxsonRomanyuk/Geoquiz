package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.LocalizedText;

public class OptionData {
    private int index;
    private LocalizedText text;

    public int getIndex() { return index; }
    public LocalizedText getText() { return text; }
}
