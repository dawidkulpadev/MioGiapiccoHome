package pl.dawidkulpa.miogiapiccohome;

import java.util.Timer;
import java.util.TimerTask;

public class TimeoutWatchdog {
    public interface TimeoutCallback{
        void onTimeout();
    }

    private Timer timeoutWatchdogTimer;
    private TimeoutCallback l=null;

    public void start(long ms, TimeoutCallback onTimeout){
        l= onTimeout;

        if(timeoutWatchdogTimer !=null){
            timeoutWatchdogTimer.cancel();
        }

        timeoutWatchdogTimer = new Timer();
        timeoutWatchdogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(l!=null)
                    l.onTimeout();
                l= null;
            }
        }, ms);
    }

    public void stop(){
        if(timeoutWatchdogTimer !=null) {
            timeoutWatchdogTimer.cancel();
            timeoutWatchdogTimer = null;
        }
    }
}
