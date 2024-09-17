package cmd;
//CMA (Camera) Editor by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MainApp 
{
	public static boolean isForWii=false;
	static float cmaDuration;
	static int cmaSectionTotal;
	static int cmaSectionsAddress;
	static int[] cmaSectionOrder = new int[6];
	static int[] cmaSectionOffsets = new int[6]; 
	static int[] cmaSectionPoints = new int[6];
	static RandomAccessFile currCMA;
	static final String CMA_PATH = "./cma/";
	static final String OUT_PATH = "./out/";
	static final String[] SECTION_NAMES = {"POS-X","POS-Y","POS-Z","ROT-Y","ROT-X","ROT-Z"};
	
	public static boolean isFaultyCMA() throws IOException
	{
		currCMA.seek(0);
		//get CMA type (PS2 or Wii), then check if file starts with CMAn or CMAw
		int cmaType = currCMA.readInt();
		if (cmaType==0x434D416E) isForWii=false;
		else if (cmaType==0x434D4177) isForWii=true;
		else return true;
		//read information that will be used for other methods later on
		cmaSectionTotal = LittleEndian.getInt(currCMA.readInt());
		cmaSectionsAddress = LittleEndian.getInt(currCMA.readInt());
		currCMA.readLong(); //skip next 8 bytes
		cmaDuration = LittleEndian.getFloat(currCMA.readFloat());
		//signature = placeholder/default/initial camera point
		int cmaSignatureOffset = LittleEndian.getInt(currCMA.readInt());
		//get camera size and compare it to the file size
		int cmaSizeWithoutHeader = LittleEndian.getInt(currCMA.readInt());
		if (cmaSizeWithoutHeader != currCMA.length()-16) return true;
		
		currCMA.seek(cmaSignatureOffset);
		long nextSectionOffset=0; int cmaSectionCnt=0;
		for (int i=0; i<cmaSectionTotal; i++)
		{
			//0x00000080 and 0x00000000 are both zero, but one is a float and the other is an integer
			if (currCMA.readInt()!=0) cmaSectionCnt++;
		}
		if (cmaSectionCnt!=cmaSectionTotal) return true;
		
		currCMA.seek(cmaSectionsAddress);
		for (int i=0; i<cmaSectionTotal; i++)
		{
			int sectionID = LittleEndian.getInt(currCMA.readInt());
			if (sectionID<0 || sectionID>5) return true;
			cmaSectionOrder[i]=sectionID; //keep track of the section IDs from top to bottom
			cmaSectionOffsets[sectionID] = LittleEndian.getInt(currCMA.readInt());
			cmaSectionPoints[sectionID] = LittleEndian.getInt(currCMA.readInt());
			currCMA.readInt(); //skip padding
			if (i>0)
			{
				if (nextSectionOffset!=cmaSectionOffsets[sectionID]) return true;
			}
			nextSectionOffset = cmaSectionOffsets[sectionID]+32*cmaSectionPoints[sectionID];
		}
		return false;
	}
	public static String readCMA() throws IOException
	{
		String sectionName=null, output="";
		int cameraPoints;
		currCMA.seek(cmaSectionOffsets[cmaSectionOrder[0]]);
		for (int i=0; i<cmaSectionTotal; i++)
		{
			cameraPoints = cmaSectionPoints[cmaSectionOrder[i]];
			sectionName = SECTION_NAMES[cmaSectionOrder[i]];
			output+="["+sectionName+"]\n";
			for (int j=0; j<cameraPoints; j++)
			{
				currCMA.readInt(); //skip first 4 bytes (a set of unknown flags)
				float keyframe = LittleEndian.getFloat(currCMA.readFloat());
				float value = LittleEndian.getFloat(currCMA.readFloat());
				output+="Value (at Frame "+keyframe+"): "+value+"\n";
				currCMA.seek(currCMA.getFilePointer()+20); //skip rest of bytes
			}
		}
		return output;
	}
	public static void writeCMA(int sectionID, float coefficient, boolean hasMultiplyArg) throws IOException
	{
		if (sectionID<0 || sectionID>5)
		{
			System.out.println("Invalid section provided! Exiting..."); return;
		}
		currCMA.seek(cmaSectionOffsets[sectionID]);
		for (int i=0; i<cmaSectionPoints[sectionID]; i++)
		{
			currCMA.readLong(); //skip first 8 bytes
			float value = LittleEndian.getFloat(currCMA.readFloat());
			currCMA.seek(currCMA.getFilePointer()-4);
			if (hasMultiplyArg) value*=coefficient;
			else value+=coefficient;
			/* write methods in RAF always write in big endian, but as seen above, I already read the value in Little Endian,
			so I had no choice but to convert the float into an integer and use the LittleEndian class to make it Big Endian */ 
			int newValue = LittleEndian.getInt(Float.floatToRawIntBits(value));
			currCMA.writeInt(newValue);
			currCMA.seek(currCMA.getFilePointer()+20); //skip rest of bytes
		}
	}
	public static void main(String[] args) throws IOException 
	{
		String helpText = "Read and write CMA contents for both PS2 & Wii versions of Budokai Tenkaichi games.\n"
		+ "Here is a list of all the arguments that can be used. Use -h or -help to print this out again.\n\n"
		+ "* -r --> Read CMA files and write their contents in *.txt files with the same file names.\n"
		+ "* -wm -> Overwrite CMA files by multiplying their contents by a coefficient.\n"
		+ "* -wa -> Overwrite CMA files by adding a coefficient to their contents.\n\n"
		+ "For the write commands, the first argument must be the number of the section that will be edited:\n"
		+ "0 = POS-X, 1 = POS-Y, 2 = POS-Z, 3 = ROT-Y, 4 = ROT-X, 5 = ROT-Z.\n"
		+ "NOTE: For this program, X is left/right, Y is up/down, Z is back/forward.\n\n"
		+ "As for the second argument, it must be a coefficient (whole number or decimal).\n";
		boolean hasReadArg=false, hasMultiplyArg=false;
		int sectionID=-1; float coefficient=1;
		
		if (args.length>3)
		{
			System.out.println("Too many arguments have been provided. Only two arguments are allowed."); System.exit(1);
		}
		else if (args.length==3)
		{
			if (args[0].equals("-wm")) 
			{
				hasMultiplyArg=true;
				sectionID=Integer.parseInt(args[1]);
				coefficient=Float.parseFloat(args[2]);
			}
			if (args[0].equals("-wa")) 
			{
				hasMultiplyArg=false;
				sectionID=Integer.parseInt(args[1]);
				coefficient=Float.parseFloat(args[2]);
			}
		}
		else if (args.length==1)
		{
			if (args[0].equals("-r")) hasReadArg=true;
			if (args[0].equals("-h"))
			{
				System.out.println(helpText); System.exit(0);
			}
		}
		else
		{
			System.out.println("A valid set of arguments must be provided. Use -h for help."); System.exit(1);
		}		
		
		File folder = new File(CMA_PATH), outputTxt;
		File[] cmaPaths = folder.listFiles((dir, name) -> (name.toLowerCase().endsWith(".cma")));
		FileWriter fw;
		RandomAccessFile[] cmaFiles = new RandomAccessFile[cmaPaths.length];
		
		int cmaIndex=0;
		for (cmaIndex=0; cmaIndex<cmaPaths.length; cmaIndex++)
			cmaFiles[cmaIndex] = new RandomAccessFile(cmaPaths[cmaIndex].getAbsolutePath(),"rw");
		
		for (cmaIndex=0; cmaIndex<cmaPaths.length; cmaIndex++)
		{
			currCMA = cmaFiles[cmaIndex];
			String fileName = cmaPaths[cmaIndex].getName();
			if (isFaultyCMA()) 
			{
				System.out.println("Skipping faulty CMA file..."); continue;
			}
			if (hasReadArg) 
			{
				System.out.println("Reading "+fileName+" contents...");
				outputTxt = new File(OUT_PATH+fileName.replace(".cma", ".txt"));
				fw = new FileWriter(outputTxt);
				String output = readCMA();
				fw.write(output);
				fw.close();
			}
			else
			{
				System.out.println("Overwriting "+fileName+"'s "+SECTION_NAMES[1]+" contents...");
				writeCMA(sectionID,coefficient,hasMultiplyArg);
			}
		}
	}
}