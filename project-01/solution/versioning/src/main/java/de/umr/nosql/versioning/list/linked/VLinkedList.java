package de.umr.nosql.versioning.list.linked;

import de.umr.nosql.versioning.list.base.storage.VersionList;

public class VLinkedList<P> implements VersionList<P> {

    private record Node<P>(P value, long timestamp, Node<P> next) {}


    private Node<P> head;

    @Override
    public void append(P p, long timestamp) {
        head = new Node<>(p, timestamp, head);
    }

    @Override
    public P findVisible(long timestamp) {
        if (head == null) {
            return null;
        }

        Node<P> node = head;

        while (node != null) {
            if (node.timestamp() <= timestamp) {
                return node.value;
            }

            node = node.next();
        }

        return null;
    }
}
