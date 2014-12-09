package me.everything.jittlib;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    private ServerAPI mServerAPI;

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mServerAPI = new ServerAPI();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mServerAPI = null;
    }

    public void testServerAPI() {
        List<String> keys = new ArrayList<String>();
        keys.add("s_aaa");
        keys.add("s_bbb");
        keys.add("hello_world");
        List<String> locales = new ArrayList<String>();
        locales.add("kl_KL");
        locales.add("en_US");
        locales.add("es_CL");
        locales.add("he_IL");
        final CountDownLatch latch = new CountDownLatch(1);
        mServerAPI.getTranslations("12345", keys, locales, new ServerAPI.ITranslationsReceiver() {
            @Override
            public void onGotTranslations(ServerAPI.TranslationResult suggestions) {
                Log.e("TTT", "GOT "+suggestions);
                latch.countDown();
            }
        });
        Log.e("TTT", "WAITING");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e("TTT", "DONE");
    }

}