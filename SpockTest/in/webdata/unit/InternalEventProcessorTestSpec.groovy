package in.webdata.unit;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.InternalEventProcessor;
import com.sapienter.jbilling.server.system.event.InternalEventProcessorTest.TestEvent
import com.sapienter.jbilling.server.system.event.InternalEventProcessorTest.TestInternalEventTask
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

import spock.lang.Specification;
import spock.lang.Shared;

import java.lang.Class;



public class InternalEventProcessorTestSpec extends Specification {

    // class under test
    @Shared public static final InternalEventProcessor processor = new InternalEventProcessor();

    	def "testIsProcessable"()  {
			
			setup:	
	        	
				TestInternalEventTask task = new TestInternalEventTask();
	
				def ar = new Class[1];
				
				ar[0]  = TestEvent.class;
	 					
				task.setSubscribedEvents(ar);

		    expect:
			
				true						 ==  processor.isProcessable(task, new TestEvent());
		}


		def "testIsProcessableNegativeCase"() {
 
			setup:
				
				def ar = new Class[1];
				ar[0]  = TestEvent.class;
			 
				   TestInternalEventTask task = new TestInternalEventTask();
				   task.setSubscribedEvents(ar);

			expect:
								
            	false					==        processor.isProcessable(task, new NewUserStatusEvent(1, 2, 3, 4));
		}

		def "testIsProcessableCatchAll"()  {
			
			setup:
			
				def ar = new Class[1];
				ar[0]  = CatchAllEvent.class;
		        TestInternalEventTask task = new TestInternalEventTask();
		        task.setSubscribedEvents(ar);
		        
			expect:
			 	
				true 					==        processor.isProcessable(task, new TestEvent());
    }

		def "testIsProcessableEmptySubscribedEvents"()  {
        
			setup:
				
				def ar = new Class[1];
				TestInternalEventTask task = new TestInternalEventTask();
				task.setSubscribedEvents(ar);

			expect:        
            
			        false               == 		processor.isProcessable(task, new TestEvent());
    }

		def "testIsProcessableNullSubscribedEvents"() {
			
			setup:
					
		        TestInternalEventTask task = new TestInternalEventTask();
		        task.setSubscribedEvents(null);
			
				expect:
					
				false				== processor.isProcessable(task, new TestEvent());
    }

    public static class TestEvent implements Event {
        public String getName() { return "test event"; }
        public Integer getEntityId() { return null; }
    }

    public static class TestInternalEventTask implements IInternalEventsTask {
        private Class<Event>[] events = [];
        
        public Class<Event>[] getSubscribedEvents() { return events; }
        public void setSubscribedEvents(Class<Event>[] events) { this.events = events; }

        public void process(Event event) throws PluggableTaskException { /* noop */ }
    }
}
