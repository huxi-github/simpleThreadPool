import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Pool {
//
private ArrayList<Thread> threads=null;  //只是用来管理的
private ArrayBlockingQueue<Runnable> taskqueue = null;
private int nthreadmax=5; //默认 5个工作线程
private int nthreadactive=0; //活动线程
	
public Pool() {
		this.taskqueue =new ArrayBlockingQueue<Runnable>(8); 
		this.threads=new ArrayList<Thread>(10);  

}
public void excuteTask(Runnable newtask){
			
		if(nthreadactive<nthreadmax) {

				//方案二： //执行任务临时new
				Thread thread=new Thread(new Runnable() {
					Runnable tempnewtask=null;
					
					@Override
					public void run() {
						this.tempnewtask=newtask;
						for(;;) {
							if(tempnewtask!=null) {
								tempnewtask.run(); //run only once;
								tempnewtask=null;
							}
							Runnable queueTask=taskqueue.poll();  //taskqueue本身是线程安全的，可以阻塞
							if(queueTask != null)
							queueTask.run();
						}
					
					}
					});
				thread.start();
				nthreadactive++;
				threads.add(thread);
		}
		else {			
					//task 已经达到maxnums已经满了,塞入任务队列
				if(!taskqueue.offer(newtask));
				System.out.println("taskqueue is full rejeact this task size="+taskqueue.size()); 
		}

}

public static void main(String[] args) {
	Pool pool=new Pool();
	Runnable newtask=new Runnable() {
		
		@Override
		public void run() {
		System.out.println("hello1");
		}
	}; 
	Runnable newtask2=new Runnable() {
		
		@Override
		public void run() {
		System.out.println("hello2");
		}
	}; 
	Runnable newtask3=new Runnable() {
		
		@Override
		public void run() {
		System.out.println("hello3");
		}
	}; 
	pool.excuteTask(newtask);
	pool.excuteTask(newtask2);
	pool.excuteTask(newtask3);
	
	pool.excuteTask(newtask);
	pool.excuteTask(newtask2);
	pool.excuteTask(newtask3);
	
	pool.excuteTask(newtask);
	pool.excuteTask(newtask2);
	pool.excuteTask(newtask3);
}
}