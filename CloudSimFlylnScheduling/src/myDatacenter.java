import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;


public class myDatacenter extends Datacenter implements MyCallInterface
{
	public static int finishFlag=0;
	private List<SimEvent> list = new ArrayList<>();
	private myThread vmThread;
	
	private List<myVm> onceSubmittedVmList = new ArrayList<myVm>();
	private List<myVm> allSubmittedVmList = new ArrayList<myVm>();
	public myDatacenter(
			String name,
			DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList,
			double schedulingInterval) throws Exception{
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		vmThread = new myThread("vmThread"); 
		vmThread.start();
	}
	@Override
	public void processEvent(SimEvent ev) {
		int srcId = -1;

		switch (ev.getTag()) {
		// Resource characteristics inquiry
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				srcId = ((Integer) ev.getData()).intValue();
				sendNow(srcId, ev.getTag(), getCharacteristics());
				break;

			// Resource dynamic info inquiry
			case CloudSimTags.RESOURCE_DYNAMICS:
				srcId = ((Integer) ev.getData()).intValue();
				sendNow(srcId, ev.getTag(), 0);
				break;

			case CloudSimTags.RESOURCE_NUM_PE:
				srcId = ((Integer) ev.getData()).intValue();
				int numPE = getCharacteristics().getNumberOfPes();
				sendNow(srcId, ev.getTag(), numPE);
				break;

			case CloudSimTags.RESOURCE_NUM_FREE_PE:
				srcId = ((Integer) ev.getData()).intValue();
				int freePesNumber = getCharacteristics().getNumberOfFreePes();
				sendNow(srcId, ev.getTag(), freePesNumber);
				break;

			// New Cloudlet arrives
			case CloudSimTags.CLOUDLET_SUBMIT:
				processCloudletSubmit(ev, false);
				break;

			// New Cloudlet arrives, but the sender asks for an ack
			case CloudSimTags.CLOUDLET_SUBMIT_ACK:
				processCloudletSubmit(ev, true);
				break;

			// Cancels a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_CANCEL:
				processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
				break;

			// Pauses a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_PAUSE:
				processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
				break;

			// Pauses a previously submitted Cloudlet, but the sender
			// asks for an acknowledgement
			case CloudSimTags.CLOUDLET_PAUSE_ACK:
				processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
				break;

			// Resumes a previously submitted Cloudlet
			case CloudSimTags.CLOUDLET_RESUME:
				processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
				break;

			// Resumes a previously submitted Cloudlet, but the sender
			// asks for an acknowledgement
			case CloudSimTags.CLOUDLET_RESUME_ACK:
				processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
				break;

			// Moves a previously submitted Cloudlet to a different resource
			case CloudSimTags.CLOUDLET_MOVE:
				processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
				break;

			// Moves a previously submitted Cloudlet to a different resource
			case CloudSimTags.CLOUDLET_MOVE_ACK:
				processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
				break;

			// Checks the status of a Cloudlet
			case CloudSimTags.CLOUDLET_STATUS:
				processCloudletStatus(ev);
				break;

			// Ping packet
			case CloudSimTags.INFOPKT_SUBMIT:
				processPingRequest(ev);
				break;

			case CloudSimTags.VM_CREATE:
				processVmCreate(ev, false);
				break;

			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev, true);
				break;
//			case CloudSimTags.VM_WaitCreate:
//				updateCloudletProcessing();
//				checkCloudletCompletion();
//				break;
			case CloudSimTags.VM_DESTROY:
				processVmDestroy(ev, false);
				break;

			case CloudSimTags.VM_DESTROY_ACK:
				processVmDestroy(ev, true);
				break;

			case CloudSimTags.VM_MIGRATE:
				processVmMigrate(ev, false);
				break;

			case CloudSimTags.VM_MIGRATE_ACK:
				processVmMigrate(ev, true);
				break;
				
		
				
			case CloudSimTags.VM_DATA_ADD:
				processDataAdd(ev, false);
				break;

			case CloudSimTags.VM_DATA_ADD_ACK:
				processDataAdd(ev, true);
				break;

			case CloudSimTags.VM_DATA_DEL:
				processDataDelete(ev, false);
				break;

			case CloudSimTags.VM_DATA_DEL_ACK:
				processDataDelete(ev, true);
				break;

			case CloudSimTags.VM_DATACENTER_EVENT:
				updateCloudletProcessing();
				checkCloudletCompletion();
				break;

			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}
	public void doingVmCreating(){ 
		SimEvent ev=null;
		Log.printLine("vmThread is running !");
		if(list.size()>0)  {
			ev = list.get(0);
			processVmCreate(ev,true);
			
		}
		
	
	}
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		Vm vm = (Vm) ev.getData();
		finishFlag++;
		getVmAllocationPolicy().allocateHostForVm(vm);
//		send(getId(), 0,CloudSimTags.VM_WaitCreate, null);
		
	}
	//从host那里得知，vm创建成功
	public void VmCreatedSuccess(myVm vm){
		boolean ack = true;
		boolean result = true;
		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), 0.1, CloudSimTags.VM_CREATE_ACK, data);
		}
	//	finishFlag--;
		//执行下一个vm创建
			Log.printLine("vm"+vm.getId()+" created!");
			
			//doingVmCreating();
	//如果创建VM成功，计算价格
		if (result) {
			double amount = 0.0;
			if (getDebts().containsKey(vm.getUserId())) {
				amount = getDebts().get(vm.getUserId());
			}
			amount += getCharacteristics().getCostPerMem() * vm.getRam();
			amount += getCharacteristics().getCostPerStorage() * vm.getSize();

			getDebts().put(vm.getUserId(), amount);

			getVmList().add(vm);

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateVmProcessing(CloudSim.clock(),
					getVmAllocationPolicy()
					.getHost(vm)
					.getVmScheduler()        
					.getAllocatedMipsForVm(vm));
			list.remove(0);
			vmThread.run();
		}
		
	}
	@Override
	public void run() {
		SimEvent ev = this.getEventBuffer()!= null ? this.getEventBuffer() : getNextEvent();
		while (ev!=null) {
			
			if(ev.getTag() == CloudSimTags.VM_CREATE_ACK)
			{
				list.add(ev);

				if(list!=null){
					if(list.size()>0){
							Log.printLine("vmThread is running ...");
							vmThread.run();
							//this.doingVmCreating();
						}
		
				}
			}
			else
			{
				processEvent(ev);
				if (this.getState() != RUNNABLE) {
					break;
				}
						
			}
			ev = getNextEvent();
			//future=null VM创建未完成
			if(ev==null && list.size()>0){
//				try {
//						synchronized (this) {
//								
//							while(list!=null && list.size()!=0)
//								{
//									this.wait();
//								}
//						}
//						
//						Log.printLine("DC is waiting for vmFinish...");
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
	
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.printLine("the 3rd thread running!");
						//vm创建未完成
						while(list!=null&&list.size()>0)
						{
							
						}
						//vm创建完成，设置ev为null
						setEventBuffer(null);
					}
				 }).start();
				
			
				
			}
		
			
			
		}
	
		
	}
	class myThread extends Thread{
		
		private String target = "sds";
	    public myThread(String string) {
			// TODO Auto-generated constructor stub
	    	super(string);
		}

		@Override
	    public void run() {
	        if (target != null) {
	            doingVmCreating();
	        }
	    }
		 
		public void stopThread(){
			target = null;
		}
	}
}
