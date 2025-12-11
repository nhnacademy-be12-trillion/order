package com.nhnacademy.order.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuth {
    AuthRole role() default AuthRole.MEMBER;
    boolean checkOrderOwner() default false;
}
