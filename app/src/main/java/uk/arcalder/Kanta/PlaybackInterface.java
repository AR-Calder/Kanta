package uk.arcalder.Kanta;

import android.media.session.MediaSession;

/**
 * Created by Zynch on 30/03/2018.
 */

/*interface MyListener{
    void somethingHappened();
}

public class MyForm implements MyListener{
    MyClass myClass;
    public MyForm(){
        this.myClass = new MyClass();
        myClass.addListener(this);
    }
    public void somethingHappened(){
       System.out.println("Called me!");
    }
}
public class MyClass{
    private List<MyListener> listeners = new ArrayList<MyListener>();

    public void addListener(MyListener listener) {
        listeners.add(listener);
    }
    void notifySomethingHappened(){
        for(MyListener listener : listeners){
            listener.somethingHappened();
        }
    }
}*/

public interface PlaybackInterface {
    // start playback
    void start();

    // stop playback
    void stop();

    // set playback state as ...
    void setPlaybackState(int playbackState);

    // get current playback state
    int getPlaybackState();

    // is anything playing?
    boolean isPlaying();

    // play a queue item
    void play(MediaSession.QueueItem queueItem);

    void pause();

    void seekTo(long position);

}
