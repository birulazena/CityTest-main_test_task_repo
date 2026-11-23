package com.citylist.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhotoDomain {
    WIKIMEDIA("wikimedia.org");

    private final String host;
}
