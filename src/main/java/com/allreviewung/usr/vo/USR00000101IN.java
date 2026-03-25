package com.allreviewung.usr.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class USR00000101IN {

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상, 255자 이하여야 합니다;")
    private String pswd;

    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100, message = "이메일은 100자를 넘을 수 없습니다;")
    private String emil;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 50, message = "닉네임은 50자를 넘을 수 없습니다;")
    private String nkNm;

}