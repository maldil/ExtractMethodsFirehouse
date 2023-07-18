package utils;


import java.util.Objects;

public class MyPair<F, S> {
    public final F first;
    public final S second;

    /**
     * Constructor for a MyPair.
     *
     * @param first the first object in the MyPair
     * @param second the second object in the pair
     */
    public MyPair(F first, S second) {
        this.first = first;
        this.second = second;
    }



    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link MyPair} to which this one is to be checked for equality
     * @return true if the underlying objects of the MyPair are both considered
     *         equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MyPair<?, ?> p)) {
            return false;
        }
        return Objects.equals(p.first, first) && Objects.equals(p.second, second);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the MyPair
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

    /**
     * Convenience method for creating an appropriately typed pair.
     * @param a the first object in the MyPair
     * @param b the second object in the pair
     * @return a MyPair that is templatized with the changegraphbuilder.types of a and b
     */
    public static <A, B> MyPair<A, B> create(A a, B b) {
        return new MyPair<A, B>(a, b);
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }


}