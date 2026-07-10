package draylar.tiered.lib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SortList {

    @SafeVarargs
    public static <T extends Comparable<? super T>> void concurrentSort(List<T> keyList, List<?>... lists) {
        if (keyList.isEmpty()) return;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < keyList.size(); i++) {
            indices.add(i);
        }

        indices.sort(Comparator.comparing(keyList::get));

        reorder(keyList, indices);
        for (List<?> list : lists) {
            if (list != keyList && list.size() == keyList.size()) {
                reorder(list, indices);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void reorder(List<T> list, List<Integer> indices) {
        List<T> copy = new ArrayList<>(list);
        for (int i = 0; i < indices.size(); i++) {
            list.set(i, copy.get(indices.get(i)));
        }
    }
}
