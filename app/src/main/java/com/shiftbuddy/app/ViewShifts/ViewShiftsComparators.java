package com.shiftbuddy.app.ViewShifts;

import com.shiftbuddy.app.Database.Tables.Shifts.Shift;

import java.util.Comparator;

/**
 * Contains all of the comparators for the ViewShifts Activity so that the table can be sorted by the requested column
 * Created by jorda_000 on 6/12/2016.
 */
public class ViewShiftsComparators {


    public static Comparator getDateComparator() {
        return new dateComparator();
    }

    public static Comparator getShiftDurationComparator() {
        return new breakComparator();
    }

    public static Comparator getBreakComparator() {
        return new breakComparator();
    }

    public static Comparator gettotalPayComparator() {
        return new totalPayComparator();
    }

    public static Comparator gettotalPayPerHourComparator(){return new totalPayPerHourComparator();}

    public static Comparator getTipsComparator() {
        return new tipsComparator();
    }

    public static Comparator getSalesComparator() {
        return new salesComparator();
    }


    private static class dateComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            return lhs.punchInDT.compareTo(rhs.punchInDT);
        }
    }

    private static class breakComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (lhs.breakTime < rhs.breakTime) return -1;
            else
                return 1;
        }
    }

    private static class shiftDurationComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (lhs.totalMinutes < rhs.totalMinutes) return -1;
            else
                return 1;
        }
    }

    private static class totalPayComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (lhs.totalPay > rhs.totalPay) return -1;
            else
                return 1;
        }
    }

    private static class totalPayPerHourComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (((lhs.totalMinutes / 60) * lhs.payPerHour) >((rhs.totalMinutes / 60) * rhs.payPerHour)) return -1;
            else
                return 1;
        }
    }

    private static class salesComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (lhs.sales > rhs.sales) return -1;
            else
                return 1;
        }
    }

    private static class tipsComparator implements Comparator<Shift> {
        @Override
        public int compare(Shift lhs, Shift rhs) {
            if (lhs.tips > rhs.tips) return -1;
            else
                return 1;
        }
    }


}
