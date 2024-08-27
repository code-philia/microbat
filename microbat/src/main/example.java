
public class example {

	
	public void addition() {
		int a = 1;
		int b = 2;
		
		int c = a + 0;
		
		assert(c == 3); // wrong variable
	}
	
	
	public boolean lessEqual() {
		int a = 1;
		int b = 1;
		
		if(a < b) {
			return true;
		}
		else {
			return false; // wrong path
		}
	}
	
	
	public int sum() {
		int s = 0; // correct step
		
		for(int i = 0; i<10 ; i++) {
			// do nothing
		}
		
		return s;
	}
	
	
}
