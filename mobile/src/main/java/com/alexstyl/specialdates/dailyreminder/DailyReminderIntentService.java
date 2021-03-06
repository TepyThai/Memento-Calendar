package com.alexstyl.specialdates.dailyreminder;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.alexstyl.android.AlarmManagerCompat;
import com.alexstyl.specialdates.BuildConfig;
import com.alexstyl.specialdates.Optional;
import com.alexstyl.specialdates.date.ContactEvent;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.events.bankholidays.BankHoliday;
import com.alexstyl.specialdates.events.bankholidays.BankHolidayProvider;
import com.alexstyl.specialdates.events.bankholidays.BankHolidaysPreferences;
import com.alexstyl.specialdates.events.bankholidays.GreekBankHolidaysCalculator;
import com.alexstyl.specialdates.events.namedays.NamedayLocale;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.events.namedays.NamesInADate;
import com.alexstyl.specialdates.events.namedays.calendar.NamedayCalendar;
import com.alexstyl.specialdates.events.namedays.calendar.OrthodoxEasterCalculator;
import com.alexstyl.specialdates.events.namedays.calendar.resource.NamedayCalendarProvider;
import com.alexstyl.specialdates.permissions.PermissionChecker;
import com.alexstyl.specialdates.service.PeopleEventsProvider;
import com.alexstyl.specialdates.date.TimePeriod;
import com.alexstyl.specialdates.util.Notifier;
import com.novoda.notils.logger.simple.Log;

import java.util.List;

/**
 * A service that looks up all events on the specified date and notifies the user about it
 */
public class DailyReminderIntentService extends IntentService {

    private static final int REQUEST_CODE = 0;
    private NamedayPreferences namedayPreferences;
    private NamedayCalendarProvider namedayCalendarProvider;

    private BankHolidaysPreferences bankHolidaysPreferences;
    private PermissionChecker checker;

    public DailyReminderIntentService() {
        super("DailyReminder");
    }

    private Notifier notifier;

    public static void startService(Context context) {
        Intent service = new Intent(context, DailyReminderIntentService.class);
        context.startService(service);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifier = Notifier.newInstance(this);
        namedayPreferences = NamedayPreferences.newInstance(this);
        namedayCalendarProvider = NamedayCalendarProvider.newInstance(this.getResources());
        bankHolidaysPreferences = BankHolidaysPreferences.newInstance(this);
        checker = new PermissionChecker(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PeopleEventsProvider provider = PeopleEventsProvider.newInstance(this);
        Date today = getDayDateToDisplay();

        if (hasContactPermission()) {
            List<ContactEvent> celebrationDate = provider.getCelebrationDateFor(TimePeriod.between(today, today));
            if (containsAnyContactEvents(celebrationDate)) {
                notifier.forDailyReminder(today, celebrationDate);
            }
        }
        if (namedaysAreEnabledForAllCases()) {
            notifyForNamedaysFor(today);
        }
        if (bankholidaysAreEnabled()) {
            notifyForBankholidaysFor(today);
        }

        DailyReminderPreferences preferences = DailyReminderPreferences.newInstance(this);
        if (preferences.isEnabled()) {
            new DailyReminderScheduler(AlarmManagerCompat.from(this), this).setupReminder(preferences);
        }
    }

    private boolean hasContactPermission() {
        return checker.canReadAndWriteContacts();
    }

    private Date getDayDateToDisplay() {
        if (BuildConfig.DEBUG) {
            DailyReminderDebugPreferences preferences = DailyReminderDebugPreferences.newInstance(this);
            if (preferences.isFakeDateEnabled()) {
                Date selectedDate = preferences.getSelectedDate();
                Log.d("Using DEBUG date to display: " + selectedDate);
                return selectedDate;
            }
        }
        return Date.today();
    }

    private boolean containsAnyContactEvents(List<ContactEvent> celebrationDate) {
        return celebrationDate.size() > 0;
    }

    private boolean namedaysAreEnabledForAllCases() {
        return namedayPreferences.isEnabled() && !namedayPreferences.isEnabledForContactsOnly();
    }

    private void notifyForNamedaysFor(Date date) {
        NamedayLocale locale = namedayPreferences.getSelectedLanguage();
        NamedayCalendar namedayCalendar = namedayCalendarProvider.loadNamedayCalendarForLocale(locale, date.getYear());
        NamesInADate names = namedayCalendar.getAllNamedayOn(date);
        if (containsNames(names)) {
            notifier.forNamedays(names.getNames(), date);
        }
    }

    private boolean bankholidaysAreEnabled() {
        return bankHolidaysPreferences.isEnabled();
    }

    private void notifyForBankholidaysFor(Date date) {
        Optional<BankHoliday> bankHoliday = findBankholidayFor(date);
        if (bankHoliday.isPresent()) {
            notifier.forBankholiday(date, bankHoliday.get());
        }
    }

    private Optional<BankHoliday> findBankholidayFor(Date date) {
        BankHolidayProvider provider = new BankHolidayProvider(new GreekBankHolidaysCalculator(OrthodoxEasterCalculator.INSTANCE));
        return provider.calculateBankHolidayOn(date);
    }

    private boolean containsNames(NamesInADate names) {
        return names.nameCount() > 0;
    }

}
