package cmd;
//CMA (Camera) Editor v1.1 by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

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
	static String lang;
	static final String CMA_PATH = "./cma/";
	static final String OUT_PATH = "./out/";
	static final String[] SECTION_NAMES = {"POS-X","POS-Y","POS-Z","ROT-Y","ROT-X","ROT-Z"};
	static final String[] CONSOLE_TEXT = 
	{	//ENGLISH: help text (0), //argument-related text (1-2), //CMA-related text (3-5)
		"Read and write CMA contents for both PS2 & Wii versions of Budokai Tenkaichi games.\n"
		+ "Here is a list of all the arguments that can be used. Use -h or -help to print this out again.\n\n"
		+ "* -r --> Read CMA files and write their contents in *.txt files with the same file names.\n"
		+ "* -wm -> Overwrite CMA files by multiplying their contents by a coefficient.\n"
		+ "* -wa -> Overwrite CMA files by adding a coefficient to their contents.\n\n"
		+ "For the write commands, the first argument must be the number of the section that will be edited:\n"
		+ "0 = POS-X, 1 = POS-Y, 2 = POS-Z, 3 = ROT-Y, 4 = ROT-X, 5 = ROT-Z.\n"
		+ "NOTE: For this program, X is left/right, Y is up/down, Z is back/forward.\n\n"
		+ "As for the second argument, it must be a coefficient (whole number or decimal).\n",
		"Too many arguments have been provided. Only two arguments are allowed.",
		"A valid set of arguments must be provided. Use -h for help.",
		"Skipping faulty CMA file ", "Reading CMA file ", "Overwriting CMA file ",
		//SPANISH (courtesy of MetalFrieza3000): help text (6), //argument-related text (7-8), //CMA-related text (9-11)
		"Lee y Escribe los contenidos de un CMA para las versiones de PS2 y Wii de los juegos de Budokai Tenkaichi.\n"
		+ "Esta es la lista de todos los argumentos que pueden usarse. Usa -h o -help para eseñar esto otra vez.\n\n"
		+ "* -r --> Lee los archivos CMA y escribe sus contenidos en archivos .txt con el mismo nombre de archivo.\n"
		+ "* -wm -> Sobrescribe los archivos CMA multiplicando sus contenidos por un coeficiente.\n"
		+ "* -wa -> Sobrescribe los archivos CMA sumando un coeficiente a sus contenidos.\n\n"
		+ "Para los comandos escritos, el primer argumento debe ser el numero de la seccion que sera editada:\n"
		+ "0 = POS-X, 1 = POS-Y, 2 = POS-Z, 3 = ROT-Y, 4 = ROT-X, 5 = ROT-Z.\n"
		+ "NOTA: Para este programa, X es Izquierda/Derecha, Y es Arriba/Abajo, Z es Atras/Delante.\n\n"
		+ "Para el Segundo Argumente, debe ser un coeficiente (Numero entero o Decimal).\n", 
		"Se han proporcionado demasiados argumentos. Solo 2 argumentos estan permitidos.", 
		"Debe proporcionarse un set de argumentos validos. Usa -h para ayuda.", 
		"Saltando archivo CMA dañado ", "Leyendo archivo CMA ", "Escribiendo archivo CMA "
	};
	
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
				if (!lang.equals("es")) output+="Value (at Frame "+keyframe+"): "+value+"\n";
				else output+="Valor (en el Punto "+keyframe+"): "+value+"\n";
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
		Locale loc = Locale.getDefault(Locale.Category.FORMAT);
		lang = loc.getLanguage();
		boolean hasReadArg=false, hasMultiplyArg=false;
		int consTextIndex=0, sectionID=-1; float coefficient=1;
		
		if (lang.equals("es")) consTextIndex+=6;
		if (args.length>3)
		{
			consTextIndex+=1;
			System.out.println(CONSOLE_TEXT[consTextIndex]); System.exit(1);
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
				System.out.println(CONSOLE_TEXT[consTextIndex]); System.exit(0);
			}
		}
		else
		{
			consTextIndex+=2;
			System.out.println(CONSOLE_TEXT[consTextIndex]); System.exit(1);
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
			consTextIndex=0;
			if (lang.equals("es")) consTextIndex+=6;
			
			currCMA = cmaFiles[cmaIndex];
			String fileName = cmaPaths[cmaIndex].getName();
			if (isFaultyCMA()) 
			{
				consTextIndex+=3;
				System.out.println(CONSOLE_TEXT[consTextIndex]+fileName); continue;
			}
			if (hasReadArg) 
			{
				consTextIndex+=4;
				System.out.println(CONSOLE_TEXT[consTextIndex]+fileName);
				outputTxt = new File(OUT_PATH+fileName.replace(".cma", ".txt"));
				fw = new FileWriter(outputTxt);
				String output = readCMA();
				fw.write(output);
				fw.close();
			}
			else
			{
				consTextIndex+=5;
				System.out.println(CONSOLE_TEXT[consTextIndex]+fileName);
				writeCMA(sectionID,coefficient,hasMultiplyArg);
			}
		}
	}
}