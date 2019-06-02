package com.example.to_do_list;

import java.util.Calendar;
import java.util.Date;

public class Utils {
    public static boolean isExpiredDate(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        Date dateSpecified = calendar.getTime();
        if (dateSpecified.before(now))
            return true;
        return false;
    }
}
