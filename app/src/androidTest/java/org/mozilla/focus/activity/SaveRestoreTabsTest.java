package org.mozilla.focus.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Toast;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public class SaveRestoreTabsTest {

    private static final TabModel TAB = new TabModel("TEST_ID", "ID_HOME", "Yahoo TW", "https://tw.yahoo.com");
    private static final TabModel TAB_2 = new TabModel("TEST_ID_2", TAB.getId(), "Google", "https://www.google.com");

    private TabsDatabase tabsDatabase;

    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();

        tabsDatabase = Inject.getTabsDatabase(null);
    }

    @Test
    public void restoreEmptyTab() {
        activityRule.launchActivity(new Intent());
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.home_screen_menu)))).check(matches(withText("0")));
    }

    @Test
    public void restoreEmptyTab_addNewTabThenRelaunch() {
        activityRule.launchActivity(new Intent());
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.home_screen_menu)))).check(matches(withText("0")));

        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        relaunchActivity();

        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("1")));
    }

    @Test
    public void restorePreviousTabs() {
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);
        AndroidTestUtils.setFocusTabId(TAB.getId());

        activityRule.launchActivity(new Intent());
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("2")));
    }

    @Test
    public void restorePreviousManyTabs() {
//        List<TabModel> tabModelList = new ArrayList<>();
//        for (int i = 0; i < 1000; i++) {
//            tabModelList.add(new TabModel(UUID.randomUUID().toString(), "ID_HOME", "Yahoo TW", "https://tw.yahoo.com"));
//        }
//
//        tabsDatabase.tabDao().insertTabs(tabModelList.toArray(new TabModel[0]));
//        AndroidTestUtils.setFocusTabId(tabModelList.get(0).getId());

        activityRule.launchActivity(new Intent());
//        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("∞")));

        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        for (int i = 0; i < 1000; i++) {
            onView(allOf(withId(R.id.btn_open_new_tab), isDescendantOfA(withId(R.id.browser_screen_menu)))).perform(click());
            SystemClock.sleep(500);
            onView(ViewMatchers.withId(R.id.main_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
            //showToast("New Tab => " + (i + 1));
        }
    }

    /**
     * Display toast
     *
     * @param message message that toast display
     */
    public void showToast(final String message) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InstrumentationRegistry.getTargetContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Test
    public void restorePreviousTabs_addNewTabThenRelaunch() {
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);
        AndroidTestUtils.setFocusTabId(TAB.getId());

        activityRule.launchActivity(new Intent());
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("2")));

        onView(ViewMatchers.withId(R.id.btn_open_new_tab)).perform(click());
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        relaunchActivity();

        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("3")));
    }

    private void relaunchActivity() {
        activityRule.finishActivity();
        activityRule.launchActivity(new Intent());
    }
}
