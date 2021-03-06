import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ResendManager implements Runnable{
    private volatile Queue<CheckAnswer> check;

    public ResendManager(Queue<CheckAnswer> check) {
        this.check = check;
    }

    @Override
    public void  run() {

        //copy to local list
        while (true) {
            List<CheckAnswer> localCopy = new ArrayList<>();
            synchronized (CheckAnswer.class) {
                CheckAnswer elem;
                while((elem = check.poll()) != null) {
                    localCopy.add(elem);
                }
            }

            int[] previousStep = new int[localCopy.size()];
            int finishedResendingCount = 0;
            for(int i = 0; i < previousStep.length; i++)
                previousStep[i] = 1;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { e.printStackTrace(); }

            for(int i = 0; i < localCopy.size(); i++) {
                localCopy.get(i).startResend();
            }

            while(finishedResendingCount < localCopy.size()) {
                for (int i = 0; i < localCopy.size(); i++) {
                    if (previousStep[i] == 1) {
                        previousStep[i] = localCopy.get(i).resend();
                        if(previousStep[i] == 0)
                            finishedResendingCount++;
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for(int i = 0; i < localCopy.size(); i++) {
                int res = -1;
                if((res = localCopy.get(i).isDead()) == 1) {
                    synchronized (CheckAnswer.class) {
                            check.offer(localCopy.get(i));
                    }
                }
            }
        }
    }
}
