package com.yifeng.hilbert;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.davidmoten.hilbert.HilbertCurve;


public class HilbertCurveTest {
    public static void main(String... args) {
        final int TOTAL_BUCKETS = 10;
        final int ROWS_PER_BUCKETS = 1000;
        final int TOTAL_ROWS = TOTAL_BUCKETS * ROWS_PER_BUCKETS;
        final int TOTAL_COLS = 2;

        long[][] records = new long[TOTAL_ROWS][TOTAL_COLS];
        HilbertCurve c = HilbertCurve.bits(32).dimensions(TOTAL_COLS);
        HashMap<BigInteger, Integer> curveIndices = new HashMap<>();
        // create hilbert curve index
        for (int i = 0; i < TOTAL_ROWS; i++) {
            for (int j = 0; j < TOTAL_COLS; j++) {
                records[i][j] = (long) (TOTAL_ROWS * TOTAL_COLS * Math.random()); // make every value unique
            }

            BigInteger addr = c.index(records[i]);
            curveIndices.put(addr, i); // Address -> Original Row Index
        }

        if (curveIndices.size() != TOTAL_ROWS) { // fail if there are duplicated addresses
            System.exit(-1);
        }

        // For each row, calculate bucket hit rate just by min/max, assuming the expression is
        // col1 = xxx AND col2 = yyy AND col3 = ZZZ
        final int colFrom = 0;//(int)(Math.random() * TOTAL_COLS);
        final int colTo = 1;//colFrom + (int)(Math.random() * (TOTAL_COLS - colFrom - 1));

        for (int rounds = 0; rounds <= 1; rounds++) {
            // Sorting by Hilbert Address
            if (rounds == 1) {
                System.out.println("Sorted by Hilbert curve");
                ArrayList<BigInteger> sortedAddrs = new ArrayList<>(curveIndices.keySet());
                Collections.sort(sortedAddrs);
                long[][] sortedRecords = new long[TOTAL_ROWS][TOTAL_COLS];
                for (int i = 0; i < TOTAL_ROWS; i++) {
                    int oldi = curveIndices.get(sortedAddrs.get(i));
                    for (int j = 0; j < TOTAL_COLS; j++) {
                        sortedRecords[i][j] = records[oldi][j];
                    }
                }
                records = sortedRecords;
            } else {
                System.out.println("Stripe hit rate before sorted by Hilbert curve");
            }

            // get stats of Min/Max of each bucket
            long[][] mins = new long[TOTAL_BUCKETS][TOTAL_COLS];
            for (int i = 0; i < TOTAL_BUCKETS; i++) {
                Arrays.fill(mins[i], Long.MAX_VALUE);
            }
            long[][] maxs = new long[TOTAL_BUCKETS][TOTAL_COLS];
            for (int i = 0; i < TOTAL_BUCKETS; i++) {
                Arrays.fill(maxs[i], Long.MIN_VALUE);
            }
            for (int i = 0; i < TOTAL_ROWS; i++) {
                int bucket = i / ROWS_PER_BUCKETS;
                for (int j = 0; j < TOTAL_COLS; j++) {
                    mins[bucket][j] = Math.min(records[i][j], mins[bucket][j]);
                    maxs[bucket][j] = Math.max(records[i][j], maxs[bucket][j]);
                }
            }

//            // print min/max of each bucket
            for (int i = 0; i < TOTAL_BUCKETS; i++) {
                for (int j = 0; j < TOTAL_COLS; j++) {
                    System.out.print("    " + mins[i][j] + " - " + maxs[i][j]);
                    System.out.print(", ");
                }
                System.out.println();
            }
            System.out.println("--------");

            // print matches

            // checkout distribution of sorted and unsorted data by
            // checking if every coordinate is located in its bucket min/max or not
            // the more the hits, the more discrete the data, the worse the locality
            double[] hits = new double[TOTAL_ROWS];
            for (int i = 0; i < TOTAL_ROWS; i++) {
                for (int bucket = 0; bucket < TOTAL_BUCKETS; bucket++) {
                    int inRange = 0;
                    for (int j = colFrom; j <= colTo; j++) {
                        if (mins[bucket][j] <= records[i][j] && records[i][j] <= maxs[bucket][j]) {
                            inRange++;
                        }
                    }
                    if (inRange == (colTo - colFrom + 1)) {
                        hits[i] += 1.0 / TOTAL_BUCKETS;
                    }
                }
            }

            System.out.printf("Searching column #%d to column #%d, stripe hit rate is %f\n",
                    colFrom, colTo, (new Percentile().evaluate(hits, 50)));
        }
    }
}

