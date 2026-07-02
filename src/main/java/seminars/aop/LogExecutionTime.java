package seminars.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает метод, время выполнения которого нужно замерить и вывести в консоль.
 * Обрабатывается ExecutionTimeAspect через Spring AOP.
 *
 * Это и есть Decorator на практике: вызывающий код получает не сам бин,
 * а CGLIB-прокси, который оборачивает вызов дополнительной логикой (замер
 * времени) и лишь затем делегирует выполнение исходному методу — снаружи
 * это неотличимо от вызова обычного метода.
 *
 * RetentionPolicy.RUNTIME обязателен — без этого аннотация была бы видна
 * только компилятору и стиралась бы из байт-кода, а Spring AOP читает
 * аннотации в рантайме через рефлексию при построении прокси.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {
}
