package com.github.ryan.facility.result;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * <b>操作结果封装类</b>
 * <p>
 * 用于封装可能成功或失败的操作结果，设计灵感来自 Rust 的 Result&lt;T, E&gt;。
 * 成功时持有结果值 T，失败时持有错误信息 E。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>不可变性：Result 一旦创建，状态不可改变</li>
 *   <li>空安全：成功值允许为 null，错误值不允许为 null</li>
 *   <li>函数式：支持 map、flatMap、recover 等链式操作</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建结果
 * Result<Integer, String> success = Result.ok(42);
 * Result<Integer, String> failure = Result.err("计算失败");
 *
 * // 链式处理
 * String message = success
 *     .map(n -> n * 2)
 *     .ensure(n -> n > 50, () -> "数值太小")
 *     .map(Object::toString)
 *     .orElse("默认值");
 *
 * // 异常捕获
 * Result<String, Exception> result = Result.of(() -> riskyOperation())
 *     .peekErr(e -> logger.error("操作失败", e))
 *     .recover(e -> "fallback");
 *
 * // 收集多个结果
 * List<Result<Integer, String>> results = ...;
 * Result<List<Integer>, String> combined = results.stream()
 *     .collect(Result.toResult());
 * }</pre>
 *
 * @param <T> 成功时的结果类型
 * @param <E> 失败时的错误类型
 */
public sealed interface Result<T, E> extends Serializable permits Result.Ok, Result.Err {

    @Serial
    long serialVersionUID = 1L;

    // ==================== 工厂方法 ====================

    /**
     * 创建成功结果
     *
     * @param value 成功值（允许为 null）
     */
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    /**
     * 创建无值的成功结果
     */
    static <E> Result<Void, E> ok() {
        return new Ok<>(null);
    }

    /**
     * 创建失败结果
     *
     * @param error 错误信息（不允许为 null）
     * @throws NullPointerException 如果 error 为 null
     */
    static <T, E> Result<T, E> err(E error) {
        Objects.requireNonNull(error, "错误信息不能为 null");
        return new Err<>(error);
    }


    /**
     * 执行 Supplier，捕获 Exception 转为 Err
     * <p>注意：Error（如 OOM）不会被捕获，会直接抛出</p>
     */
    static <T> Result<T, Exception> of(ThrowableSupplier<T> supplier) {
        Objects.requireNonNull(supplier);
        try {
            return ok(supplier.get());
        } catch (Exception e) {
            return err(e);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Uncaught Throwable in Result.of", t);
        }
    }

    /**
     * 执行 Runnable，捕获 Exception 转为 Err（无返回值）
     * <p>注意：Error（如 OOM）不会被捕获，会直接抛出</p>
     */
    static Result<Void, Exception> ofRunnable(ThrowableRunnable runnable) {
        Objects.requireNonNull(runnable);
        try {
            runnable.run();
            return ok();
        } catch (Exception e) {
            return err(e);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Uncaught Throwable in Result.ofRunnable", t);
        }
    }

    /**
     * 从 Optional 创建 Result
     *
     * @param optional    Optional 值
     * @param errorIfEmpty 值为空时的错误
     */
    static <T, E> Result<T, E> fromOptional(Optional<T> optional, Supplier<E> errorIfEmpty) {
        Objects.requireNonNull(optional);
        Objects.requireNonNull(errorIfEmpty);
        return optional.<Result<T, E>>map(Result::ok)
                .orElseGet(() -> err(errorIfEmpty.get()));
    }

    /**
     * 从可空值创建 Result
     *
     * @param nullable    可能为 null 的值
     * @param errorIfNull 值为 null 时的错误
     */
    static <T, E> Result<T, E> fromNullable(T nullable, Supplier<E> errorIfNull) {
        Objects.requireNonNull(errorIfNull);
        return nullable != null ? ok(nullable) : err(errorIfNull.get());
    }

    // ==================== 状态查询 ====================

    /**
     * 是否成功
     */
    boolean isOk();

    /**
     * 是否失败
     */
    default boolean isErr() {
        return !isOk();
    }

    /**
     * 成功且值满足条件
     */
    default boolean isOkAnd(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return isOk() && predicate.test(get());
    }

    /**
     * 失败且错误满足条件
     */
    default boolean isErrAnd(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate);
        return isErr() && predicate.test(getErr());
    }

    // ==================== 值获取 ====================

    /**
     * 获取成功值，失败时抛出异常
     *
     * @throws NoSuchElementException 如果是失败结果
     */
    T get();

    /**
     * 获取错误值，成功时抛出异常
     *
     * @throws NoSuchElementException 如果是成功结果
     */
    E getErr();

    /**
     * 获取成功值，失败时返回默认值
     */
    default T orElse(T defaultValue) {
        return isOk() ? get() : defaultValue;
    }

    /**
     * 获取成功值，失败时通过函数计算默认值
     */
    default T orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        return isOk() ? get() : supplier.get();
    }

    /**
     * 获取成功值，失败时根据错误计算默认值
     */
    default T orElseMap(Function<? super E, ? extends T> function) {
        Objects.requireNonNull(function);
        return isOk() ? get() : function.apply(getErr());
    }

    /**
     * 获取成功值，失败时抛出指定异常
     */
    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isOk()) {
            return get();
        }
        throw exceptionSupplier.get();
    }

    /**
     * 获取成功值，失败时根据错误抛出异常
     */
    default <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X {
        if (isOk()) {
            return get();
        }
        throw exceptionMapper.apply(getErr());
    }

    /**
     * 类似 Rust 的 unwrap，获取值或抛出异常
     * <p>语义：我确信这是成功的，否则就崩溃</p>
     */
    default T unwrap() {
        return get();
    }

    /**
     * 类似 Rust 的 unwrap_err，获取错误或抛出异常
     */
    default E unwrapErr() {
        return getErr();
    }

    // ==================== 转换操作 ====================

    /**
     * 对成功值进行转换
     */
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    /**
     * 对失败值进行转换
     */
    <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper);

    /**
     * 同时转换成功值和失败值
     */
    default <U, F> Result<U, F> bimap(
            Function<? super T, ? extends U> okMapper,
            Function<? super E, ? extends F> errMapper) {
        Objects.requireNonNull(okMapper);
        Objects.requireNonNull(errMapper);
        if (isOk()) {
            return ok(okMapper.apply(get()));
        }
        return err(errMapper.apply(getErr()));
    }

    /**
     * 对成功值进行扁平化转换
     */
    default <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
        Objects.requireNonNull(mapper);
        if (isErr()) {
            @SuppressWarnings("unchecked")
            Result<U, E> self = (Result<U, E>) this;
            return self;
        }
        Result<U, E> result = mapper.apply(get());
        return Objects.requireNonNull(result, "flatMap 返回值不能为 null");
    }

    /**
     * 确保值满足条件，否则返回错误
     * <p>语义：校验业务规则</p>
     *
     * @param predicate     校验条件
     * @param errorSupplier 不满足条件时的错误提供者
     */
    default Result<T, E> ensure(Predicate<? super T> predicate, Supplier<? extends E> errorSupplier) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(errorSupplier);
        if (isErr()) {
            return this;
        }
        return predicate.test(get()) ? this : err(errorSupplier.get());
    }

    /**
     * filter 的别名，功能同 {@link #ensure}
     */
    default Result<T, E> filter(Predicate<? super T> predicate, Supplier<? extends E> errorSupplier) {
        return ensure(predicate, errorSupplier);
    }

    /**
     * 从失败中恢复，返回新的成功值
     */
    default Result<T, E> recover(Function<? super E, ? extends T> recoveryFunction) {
        Objects.requireNonNull(recoveryFunction);
        if (isOk()) {
            return this;
        }
        return ok(recoveryFunction.apply(getErr()));
    }

    /**
     * 从失败中恢复，返回新的 Result
     */
    default Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> recoveryFunction) {
        Objects.requireNonNull(recoveryFunction);
        if (isOk()) {
            return this;
        }
        Result<T, E> result = recoveryFunction.apply(getErr());
        return Objects.requireNonNull(result, "recoverWith 返回值不能为 null");
    }

    /**
     * 交换成功和失败
     */
    default Result<E, T> swap() {
        return isOk() ? err(get()) : ok(getErr());
    }

    // ==================== 组合操作 ====================

    /**
     * 如果当前成功，返回 other；否则返回当前错误
     */
    default <U> Result<U, E> and(Result<U, E> other) {
        Objects.requireNonNull(other);
        if (isErr()) {
            @SuppressWarnings("unchecked")
            Result<U, E> self = (Result<U, E>) this;
            return self;
        }
        return other;
    }

    /**
     * 如果当前成功，执行 supplier 返回新 Result；否则返回当前错误
     */
    default <U> Result<U, E> andThen(Supplier<? extends Result<U, E>> supplier) {
        Objects.requireNonNull(supplier);
        if (isErr()) {
            @SuppressWarnings("unchecked")
            Result<U, E> self = (Result<U, E>) this;
            return self;
        }
        return Objects.requireNonNull(supplier.get(), "andThen 返回值不能为 null");
    }

    /**
     * 如果当前失败，返回 other；否则返回当前成功值
     */
    default Result<T, E> or(Result<T, E> other) {
        Objects.requireNonNull(other);
        return isOk() ? this : other;
    }

    /**
     * 如果当前失败，执行 supplier 返回新 Result；否则返回当前成功值
     */
    default Result<T, E> orElseSupplier(Supplier<? extends Result<T, E>> supplier) {
        Objects.requireNonNull(supplier);
        if (isOk()) {
            return this;
        }
        return Objects.requireNonNull(supplier.get(), "orElse 返回值不能为 null");
    }

    // ==================== 副作用操作 ====================

    /**
     * 成功时执行操作（查看成功值），返回自身用于链式调用
     */
    default Result<T, E> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (isOk()) {
            action.accept(get());
        }
        return this;
    }

    /**
     * 失败时执行操作（查看错误值），返回自身用于链式调用
     */
    default Result<T, E> peekErr(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        if (isErr()) {
            action.accept(getErr());
        }
        return this;
    }

    /**
     * 成功时执行操作
     */
    default void ifOk(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (isOk()) {
            action.accept(get());
        }
    }

    /**
     * 失败时执行操作
     */
    default void ifErr(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        if (isErr()) {
            action.accept(getErr());
        }
    }

    /**
     * 根据结果状态执行不同操作
     */
    default void match(Consumer<? super T> okAction, Consumer<? super E> errAction) {
        Objects.requireNonNull(okAction);
        Objects.requireNonNull(errAction);
        if (isOk()) {
            okAction.accept(get());
        } else {
            errAction.accept(getErr());
        }
    }

    /**
     * 根据结果状态返回不同值（模式匹配）
     */
    default <U> U fold(Function<? super T, ? extends U> okMapper, Function<? super E, ? extends U> errMapper) {
        Objects.requireNonNull(okMapper);
        Objects.requireNonNull(errMapper);
        return isOk() ? okMapper.apply(get()) : errMapper.apply(getErr());
    }

    // ==================== 互操作 ====================

    /**
     * 转换为 Optional（仅保留成功值）
     */
    default Optional<T> toOptional() {
        return isOk() ? Optional.ofNullable(get()) : Optional.empty();
    }

    /**
     * 错误转换为 Optional
     */
    default Optional<E> toOptionalErr() {
        return isErr() ? Optional.of(getErr()) : Optional.empty();
    }

    /**
     * 转换为 Stream（仅保留成功值）
     */
    default Stream<T> stream() {
        return isOk() ? Stream.ofNullable(get()) : Stream.empty();
    }

    // ==================== Collectors ====================

    /**
     * 短路收集：将 Stream&lt;Result&lt;T, E&gt;&gt; 转换为 Result&lt;List&lt;T&gt;, E&gt;
     * <p>
     * 只要遇到一个 Err，最终结果就是那个 Err。只有全为 Ok，最终结果才是 Ok(List)。
     * </p>
     * <p>注意：由于 Stream.collect 机制限制，不会停止流的遍历，但会丢弃后续成功结果。</p>
     *
     * <pre>{@code
     * List<Result<Integer, String>> results = List.of(
     *     Result.ok(1),
     *     Result.err("失败"),
     *     Result.ok(3)
     * );
     * Result<List<Integer>, String> combined = results.stream()
     *     .collect(Result.toResult());
     * // 结果: Err("失败")
     * }</pre>
     */
    static <T, E> Collector<Result<T, E>, ?, Result<List<T>, E>> toResult() {
        class Accumulator {
            List<T> list = new ArrayList<>();
            E firstError = null;
        }

        return Collector.of(
                Accumulator::new,
                (acc, result) -> {
                    if (acc.firstError != null) return;
                    if (result.isErr()) {
                        acc.firstError = result.getErr();
                        acc.list = null;
                    } else {
                        acc.list.add(result.get());
                    }
                },
                (acc1, acc2) -> {
                    if (acc1.firstError != null) return acc1;
                    if (acc2.firstError != null) return acc2;
                    acc1.list.addAll(acc2.list);
                    return acc1;
                },
                acc -> acc.firstError != null ? err(acc.firstError) : ok(acc.list)
        );
    }

    /**
     * 全量收集：收集所有结果，将成功值和失败值分别放入两个列表
     * <p>适用于需要处理所有结果（如批处理报告）的场景。</p>
     *
     * @return 如果有错误返回 Err(List&lt;E&gt;)，否则返回 Ok(List&lt;T&gt;)
     */
    static <T, E> Collector<Result<T, E>, ?, Result<List<T>, List<E>>> collectAll() {
        class Accumulator {
            final List<T> successes = new ArrayList<>();
            final List<E> failures = new ArrayList<>();
        }

        return Collector.of(
                Accumulator::new,
                (acc, result) -> {
                    if (result.isOk()) {
                        acc.successes.add(result.get());
                    } else {
                        acc.failures.add(result.getErr());
                    }
                },
                (acc1, acc2) -> {
                    acc1.successes.addAll(acc2.successes);
                    acc1.failures.addAll(acc2.failures);
                    return acc1;
                },
                acc -> acc.failures.isEmpty() ? ok(acc.successes) : err(acc.failures)
        );
    }

    // ==================== 函数式接口 ====================

    /**
     * 可抛出异常的 Supplier
     */
    @FunctionalInterface
    interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }

    /**
     * 可抛出异常的 Runnable
     */
    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Throwable;
    }

    /**
     * 可抛出异常的 Function
     */
    @FunctionalInterface
    interface ThrowableFunction<T, R> {
        R apply(T t) throws Throwable;
    }

    // ==================== 成功实现 ====================

    record Ok<T, E>(T value) implements Result<T, E> {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public E getErr() {
            throw new NoSuchElementException("调用 getErr() 但 Result 是 Ok");
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper);
            return new Ok<>(mapper.apply(value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            // Ok 不持有 E，可以安全地复用实例
            return (Result<T, F>) this;
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
    }

    // ==================== 失败实现 ====================

    record Err<T, E>(E error) implements Result<T, E> {

        @Serial
        private static final long serialVersionUID = 1L;

        public Err {
            Objects.requireNonNull(error, "错误信息不能为 null");
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public T get() {
            if (error instanceof Throwable t) {
                throw new NoSuchElementException("调用 get() 但 Result 是 Err: " + t.getMessage(), t);
            }
            throw new NoSuchElementException("调用 get() 但 Result 是 Err: " + error);
        }

        @Override
        public E getErr() {
            return error;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            // Err 不持有 T，可以安全地复用实例
            return (Result<U, E>) this;
        }

        @Override
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            Objects.requireNonNull(mapper);
            return new Err<>(mapper.apply(error));
        }

        @Override
        public String toString() {
            return "Err(" + error + ")";
        }
    }
}
