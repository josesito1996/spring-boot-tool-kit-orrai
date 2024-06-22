package com.library.support.orrai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ErrorResponse {

    private Integer statusCode;

    private String message;

}
