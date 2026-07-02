package seminars.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Перехватывает вызовы любых методов, помеченных @LogExecutionTime, замеряет
 * время выполнения и печатает результат в консоль.
 *
 * @Around — самый "сильный" вид совета (advice): он оборачивает вызов
 * целиком, поэтому может замерить время и до, и после joinPoint.proceed().
 * Логирование вынесено в finally, чтобы время печаталось даже если метод
 * выбросил исключение — сам факт ошибки при этом не скрывается, исключение
 * продолжает throws-цепочку как ни в чём не бывало.
 *
 * Важный нюанс Spring AOP (proxy-based): перехватываются только вызовы
 * СНАРУЖИ бина (через прокси). Если аннотированный метод вызывает другой
 * аннотированный метод того же бина через this.someMethod(...), этот
 * внутренний вызов прокси не пройдёт и аспект не сработает — классическая
 * ловушка self-invocation, о которой стоит знать.
 */
@Aspect
@Component
public class ExecutionTimeAspect {

    @Around("@annotation(seminars.aop.LogExecutionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startNanos = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            System.out.println("⏱ " + methodName + " выполнен за " + elapsedMillis + " мс");
        }
    }
}
