package org.moshang.tempusetchaos.client.gui.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Translation {
    String locale() default "en_us";
    String text();
}
