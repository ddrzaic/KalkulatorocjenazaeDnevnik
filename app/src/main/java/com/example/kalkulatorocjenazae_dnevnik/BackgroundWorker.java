package com.example.kalkulatorocjenazae_dnevnik;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundWorker extends Worker {

    String currentClassHref="";
    String username="";
    String passwd="";
    ArrayList<String> user;
    String loginFormUrl = "https://ocjene.skole.hr/pocetna/posalji/";
    String eDnevnik = "https://ocjene.skole.hr";
    public BackgroundWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Worker.Result doWork() {

        try {
            user=FileIO.readArrayListFromFile("user",getApplicationContext());
            username=AESCrypt.decrypt(user.get(0));
            passwd=AESCrypt.decrypt(user.get(1));
            currentClassHref=FileIO.readFromFile(getApplicationContext(),"CurrentClassHref");
        } catch (FileNotFoundException e) {
            return Worker.Result.failure();
        } catch (Exception e) {
            return Worker.Result.failure();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                HTTPSConnection http = new HTTPSConnection();

                CookieHandler.setDefault(new CookieManager());
                String page = null;
                try {
                    page = http.GetPageContent(loginFormUrl);
                    String postParams = http.getFormParams(page, username, passwd);
                    http.sendPost(loginFormUrl, postParams);
                    String result = http.GetPageContent(eDnevnik+currentClassHref);

                    if(!result.contains("Pristup je dozvoljen isključivo korisnicima registriranim u sustavu")) {
                        Document doc= Jsoup.parse(result);
                        boolean hasNewGrades=false;
                        if(!doc.getElementsByAttributeValueContaining("href","nove").isEmpty())hasNewGrades=true;
                        if(hasNewGrades){
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);;
                            int id = createID();

                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "eDnevnik")
                                    .setSmallIcon(R.drawable.ic_notify)
                                    .setContentTitle("eDnevnik")
                                    .setContentText("Nove ocjene!")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText("Upisane nove ocjene!"))
                                    .setAutoCancel(true);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                            notificationManager.notify(id, mBuilder.build());
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        return Worker.Result.success();
    }

    public int createID(){
        Date now = new Date();
        int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));
        return id;
    }
}