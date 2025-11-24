package de.umr.nosql.versioning.list.frugal;

import de.umr.nosql.versioning.list.base.storage.VersionList;
import lombok.Builder;

public class FrugalSkipList<P> implements VersionList<P> {

    @Builder
    private record Node<P> (
            P payload,
            long timestamp,
            int level,
            Node<P> next,
            Node<P> vRidgy
    ) {}

    private Node<P> vBase;

    @Override
    public void append(P p, long timestamp) {
        if (vBase == null) {
            initStack(p, timestamp);
            return;
        }

        CoinFace coinTossResult = CoinFace.toss();

        if (coinTossResult == CoinFace.HEAD) {
            putOnTopVersionStack(p, timestamp);
        } else if (coinTossResult == CoinFace.TAIL) {
            createNewVersionStack(p, timestamp);
        }
    }

    @Override
    public P findVisible(long timestamp) {
        // Temporary node for traversing the Frugal Skip List
        Node<P> vTmp = vBase;

        // Implementation of the Algorithm 2 in the paper mentioned in the sheet
        while (vTmp != null && vTmp.timestamp() > timestamp) {
            if (vTmp.vRidgy() != null && vTmp.vRidgy().timestamp() > timestamp) {
                vTmp = vTmp.vRidgy();
            } else {
                vTmp = vTmp.next();
            }
        }

        return vTmp != null ? vTmp.payload() : null;
    }

    private void initStack(P payload, long timestamp) {
        vBase = Node.<P>builder()
                .payload(payload)
                .timestamp(timestamp)
                .level(0)
                .build();
    }

    private void putOnTopVersionStack(P payload, long timestamp) {
        // Level of the new vNew node
        int vNewLevel = vBase.level() + 1;

        // Temporary node for traversing the Frugal Skip List
        Node<P> vTmp = vBase;

        // Search for a ridgy processor by traversing the ridgy links
        while (vTmp != null) {
            if (vTmp.level() >= vNewLevel)  {
                break;
            }

            vTmp = vTmp.vRidgy();
        }

        // Construct the new vNew node and assign it to vBase
        vBase = Node.<P>builder()
                .payload(payload)
                .timestamp(timestamp)
                .level(vNewLevel)
                .next(vBase)
                .vRidgy(vTmp)
                .build();
    }

    private void createNewVersionStack(P payload, long timestamp) {
        vBase = Node.<P>builder()
                .payload(payload)
                .timestamp(timestamp)
                .level(0)
                .next(vBase)
                .vRidgy(vBase)
                .build();
    }
}
