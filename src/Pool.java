import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class Pool {
private ArrayList<Thread> threads=null;  //用来管理[销毁/释放内存]
private ArrayBlockingQueue<Runnable> taskqueue = null;
private int nthreadmax=5;				 //默认 5个工作线程
private int nthreadactive=0; 			//活动线程
private volatile boolean destroyed=false; //销毁标志
private Object objcondition=new Object(); // 共享变量

public Pool() {
		this.taskqueue =new ArrayBlockingQueue<Runnable>(8); 
		this.threads=new ArrayList<Thread>(10);  

}
public Pool(int threadNum) {
	this.nthreadmax=threadNum;
	this.taskqueue =new ArrayBlockingQueue<Runnable>(8); 
	this.threads=new ArrayList<Thread>(10);  
}
public Pool(int threadNum,int taskQueSize) {
	this.nthreadmax=threadNum;
	this.taskqueue =new ArrayBlockingQueue<Runnable>(taskQueSize); 
	this.threads=new ArrayList<Thread>(threadNum*2);  
}

public void excuteTask(Runnable newtask){
			
		if(nthreadactive<nthreadmax) {
				Thread thread=new Thread(new Runnable() {
					Runnable tempnewtask=null;
					
					@Override
					public void run() {
						this.tempnewtask=newtask;
						for(;;)  {
							
							if(destroyed) break;  //销毁的线程池 不会执行任何任务
								
							if(tempnewtask!=null) {
								tempnewtask.run(); //run only once;
								tempnewtask=null;
							}
							//taskqueue本身是线程安全的，可以阻塞
							Runnable queueTask=taskqueue.poll();  
							if(queueTask == null)
								synchronized (objcondition)
								{
											try {
												while(taskqueue.size()==0&&!destroyed) //1.size==0,让该线程，wait.不要空循环了   //2.destroyed==true 也不要wait 了
													objcondition.wait();    //condition 为几个线程共有的，方便通信      ||该方法会释放锁
												   
												
												if(destroyed) {
													System.out.println(Thread.currentThread().getName()+"is destroyed"); 
													break;
												}
											} catch (InterruptedException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
								}else    //任务执行前，释放锁
								{
									queueTask.run();  //函数运行过程不阻塞,不需要锁
								}
						}
					
					}
					});
				thread.start();
				nthreadactive++;
				threads.add(thread);
		}
		else synchronized (objcondition){			
					//task 已经达到maxnums已经满了,塞入任务队列
				if(!taskqueue.offer(newtask))
				System.out.println("taskqueue is full rejeact this task |size="+taskqueue.size()+"remaining"+taskqueue.remainingCapacity() ); 
				else {
//					System.out.println("add a task to taskqueue   |currentsize="+taskqueue.size()+"remaining"+taskqueue.remainingCapacity() );
				
					System.out.println(" awake a thread");  //内部会依次检查，找到一个线程 让其 awake
					objcondition.notify();
				}
		}

}

public void destroyPool() {  //java 里面自动回收内存，故秩序同步线程即可 //
	
	synchronized (objcondition) {     //主线程等待 获取 锁 //需要子线程释放
	this.destroyed=true;
	objcondition.notifyAll(); 		//通知 其他休眠的线程，awake 再退出
	}
}

public static void main(String[] args) throws InterruptedException { 
	Pool pool=new Pool(3);
	Runnable newtask=new Runnable() {	
		@Override
		public void run() {
		System.out.println("hello1");
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("ok1");
		}
	}; 
	Runnable newtask2=new Runnable() {	
		@Override
		public void run() {
		System.out.println("hello2");
		try {
			Thread.sleep(40);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("ok2");
		}
	}; 
	Runnable newtask3=new Runnable() {
		@Override
		public void run() {
		System.out.println("hello3");
		System.out.println("ok3");
		}
	}; 

	for (int i = 0; i < 10; i++) {
		System.out.println("add 3 task");
		pool.excuteTask(newtask);
		pool.excuteTask(newtask2);
		pool.excuteTask(newtask3);
		Thread.sleep(10);
		
	}

	Thread.sleep(1000);

	System.out.println("trying to destroy...");
	pool.destroyPool();

}
}
