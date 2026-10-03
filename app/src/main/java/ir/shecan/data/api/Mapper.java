package ir.shecan.data.api;

public interface Mapper<F, T> {
    T map(F input);
}