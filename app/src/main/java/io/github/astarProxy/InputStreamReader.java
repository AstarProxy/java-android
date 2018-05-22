package io.github.astarProxy;
/**
 * Created by maryam on 5/19/2018.
 */
import java.io.IOException;
import java.io.InputStream;
public class InputStreamReader extends Thread {
    protected InputStream stream;
    protected AvailableListener availableListener;
    protected CloseListener closeListener;

    public InputStreamReader( InputStream stream ){
        this.stream = stream;
    }

    public void setAvailableListener( AvailableListener listener ) {
        availableListener = listener;
    }
    public void setCloseListener( CloseListener listener ) {
        closeListener = listener;
    }
    @Override
    public void run(){
        try {
            while(!isInterrupted()){
                byte[] bytes = new byte[256]; //make this whatever you need
                int length = stream.read( bytes );
                if (length == -1) {
                    return;
                }
                if (availableListener != null) {
                    availableListener.onAvailable(bytes, length);
                }
            }
        } catch(IOException e) {
            if (closeListener != null) {
                closeListener.onClose();
            }
        }
    }
    public interface AvailableListener {
        public void onAvailable(byte[] bytes, int length) throws IOException;
    }
    public interface CloseListener {
        public void onClose();
    }
}