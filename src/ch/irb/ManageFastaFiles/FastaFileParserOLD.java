package ch.irb.ManageFastaFiles;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.MatchResult;

import org.apache.log4j.Logger;

import ch.irb.translation.Translator;

/**
 * @author Mathilde This class parses a fasta file it will store the information like the following: IgName -> sequence
 */
public class FastaFileParserOLD {

	// Define a static logger
	static Logger logger = Logger.getLogger(FastaFileParserOLD.class);
	private HashMap<String,String> fastaIdToSequence = new HashMap<>();
	private String filePath = null;
	private boolean simpleFastaId = false;
	private boolean isImgtFormat = false;
	private boolean isDNA = true;


	public FastaFileParserOLD(String filePath, boolean simpleFastaId, boolean isDNA) throws FastaFormatException,
			IOException {
		setDNA(isDNA);
		setSimpleFastaId(simpleFastaId);
		setFilePath(filePath);
		parseFile();
	}

	public FastaFileParserOLD(String filePath) throws FastaFormatException, IOException {
		this.isImgtFormat = true;
		setFilePath(filePath);
		parseFile();
	}

	/**
	 * @return the simpleFastaId
	 */
	public boolean isSimpleFastaId() {
		return simpleFastaId;
	}

	/**
	 * @param simpleFastaId
	 *            the simpleFastaId to set
	 */
	public void setSimpleFastaId(boolean simpleFastaId) {
		this.simpleFastaId = simpleFastaId;
	}

	/**
	 * @return the isDNA
	 */
	public boolean isDNA() {
		return isDNA;
	}

	/**
	 * @param isDNA
	 *            the isDNA to set
	 */
	public void setDNA(boolean isDNA) {
		this.isDNA = isDNA;
	}

	@SuppressWarnings("resource")
	private void parseFile() throws FastaFormatException, IOException {
		// Note that FileReader is used, not File, since File is not Closeable
		Scanner scanner = new Scanner(new FileReader(filePath));
		if (scanner.findInLine(">") == null) {
			logger.fatal("Go there for " + getFilePath());
			String message = "Wrong fasta format for the file " + getFileName()
					+ "!\n The format should be: >name cellType vaccinationYear vaccinationDays \n ";
			message += "sequence" + "\ni.e.: >FI1013VH B post09 d19 \n"
					+ "GTGCAGTCTGGGGCTGAGGTGAGGAAGCCTGGGTCCTCGGTGAGGGTCTCCTGCAAG";
			throw new FastaFormatException(message);
		}
		scanner.useDelimiter(">");

		// first use a Scanner to get each line
		while (scanner.hasNext()) {
			processEntry(scanner.next());
		}
		scanner.close();
	}

	@SuppressWarnings("resource")
	private void processEntry(String fastaEntry) throws FastaFormatException {
		String cleanedFastaEntry = fastaEntry.replaceFirst("(\r\n|\n)", "_endOfFastaId_").trim();
		// We get the fastaId
		int index = cleanedFastaEntry.indexOf("_endOfFastaId_");
		String id = cleanedFastaEntry.substring(0, index);
		if (isSimpleFastaId())
			id = (cleanedFastaEntry.substring(0, index)).toUpperCase();
		String fastaId = id.replaceAll("(DL|P1);", "").trim();// this comes from ClustalW
		id = fastaId;
		String cleaned = cleanedFastaEntry.replaceAll("(\r\n|\n|\\s+)", "").replaceAll("\\*", "")
				.replaceAll("(DL|P1);", "");
		//logger.fatal("Cleaned fasta entry: " + cleaned + "/");
		Scanner scanner = new Scanner(cleaned);

		boolean goOn = true;
		if (isDNA && (scanner.findInLine("_endOfFastaId_[-\\.acgtnACGTN]+\\s*$") == null)) {
			goOn = false;
			String message = "No nucleotide sequence found in " + getFileName();
			throw new FastaFormatException(message);
		}
		if (!isDNA && (scanner.findInLine("_endOfFastaId_[-AGTNILVFMCPSYWQHEDKRX]+\\s*$") == null)) {
			goOn = false;
			String message = "No protein sequence found in " + getFileName();
			throw new FastaFormatException(message);
		}
		if (!isImgtFormat) {
			if (cleaned.matches(".*\\..*")) // be careful if the user has selected the file with the IMGT
											// format within the selected clones
				goOn = false;
		}

		if (goOn) {
			MatchResult result = scanner.match();
			String sequence = result.group();
			// all the sequences will be in UPPER case
			String sequenceInUpperCase = sequence.replaceAll("_endOfFastaId_", "").toUpperCase();
			// logger.warn("Sequence found:" + sequenceInUpperCase);
			if (sequenceInUpperCase.matches(".*N.*") && isDNA) {
				String message = "Wrong fasta format for the file " + getFileName()
						+ "!\n There are some 'N' in the sequence!";
				throw new FastaFormatException(message);
			}
			if (sequenceInUpperCase.matches(".*[ILVFMPSYWQHEDKRX].*") && isDNA) {
				String message = "No nucleotide sequence [ACGT] found in " + getFileName() ;
				throw new FastaFormatException(message);
			}
			// logger.warn("Fasta Id To upper case is: "+id);
			//to get rid or not of VH or VL or VK in the id
		/*	if (id.matches("(.*)VH(.*)")) {
				fastaId = id.substring(0, id.indexOf("VH")) + id.substring(id.indexOf("VH") + 2, id.length());
			} else if (id.matches("(.*)VL(.*)"))
				fastaId = id.substring(0, id.indexOf("VL")) + id.substring(id.indexOf("VL") + 2, id.length());
			else if (id.matches("(.*)VK(.*)"))
				fastaId = id.substring(0, id.indexOf("VK")) + id.substring(id.indexOf("VK") + 2, id.length());*/
			// for silvia
			// else if (id.matches("\\d+_.+_POS_D21"))
			// fastaId = id.substring(0, id.indexOf("_POS"));

			if (!isImgtFormat) {
				if (!isSimpleFastaId() && !isFastaIdOk(fastaId)) { 
					String message = "For the file "
							+ getFileName()
							+ ", the fasta Id '>"
							+ fastaId
							+ "' is not in the correct format! \n The format should be: '>name cellType vaccinationYear vaccinationDays' (i.e.: >FI1013VH B post09 d19).";
					logger.fatal(message);
					throw new FastaFormatException(message);
				} else if (isSimpleFastaId()) {
					if (fastaId.matches(".*\\(.*") || fastaId.matches(".*\\).*")) { // fastaId.matches(".*\\s.*") ||
						String message = "For the file " + getFileName() + ", the fasta Id '>" + fastaId
								+ "' is not in the correct format! Symbols '(' ')'are not allowed.";
						throw new FastaFormatException(message);
					}
				}
				if (!areDeletionsOK(sequenceInUpperCase)) {
					String message = "After running CLUSTALW, the fastaId " + fastaId
							+ " has deletions but they are not a multiple of 3, coulndt process!!!";
					throw new FastaFormatException(message);
				}
				//we will not translate the entire sequence
				if (!isTheNumberOfNucleotidesAMultipleOf3(sequenceInUpperCase) && isDNA) {
					@SuppressWarnings("unused")
					String message = "After running ClustalW, the sequence of " + fastaId
							+ " has a number of nucleotides which is not a multiple of 3, coulndt process!!!";
					//throw new FastaFormatException(message);
				}
				if (!areTheDeletionsInTheFrame(sequenceInUpperCase) && isDNA) {
					String message = "After running CLUSTALW, the fastaId " + fastaId
							+ " has deletions but they are not in the frame, coulndt process!!!";
					throw new FastaFormatException(message);
				}
				if (isThereAStopCodon(sequenceInUpperCase)){
					String message ="There is a STOP codon in the related protein sequence of the fasta ID: "+fastaId+", coulndt process!!!";
					throw new FastaFormatException(message);
				}
			} else {// IMGT format, we check that we have the 6 numbers for the CDR and FR
				if (!areNumbersForCDROk(fastaId)) {
					String message = "Wrong IMGT format for the fastaID " + fastaId
							+ "!\n The format should be \n>FI457VH PC post09 d11 75 99 150 174 288 342" +
							"\ncaggtgcagctggtgcagtctggggct...gagttgaagaagcctgggtcctcggtgaag";
					throw new FastaFormatException(message);
				}
				if (!isImgtFormatOK(sequenceInUpperCase)) {
					String message = "Wrong IMGT format for the fastaID " + fastaId
							+ "!\n The format should be \n>FI457VH PC post09 d11 75 99 150 174 288 342" +
							"\ncaggtgcagctggtgcagtctggggct...gagttgaagaagcctgggtcctcggtgaag";
					throw new FastaFormatException(message);
				}
			}
			fastaIdToSequence.put(fastaId, sequenceInUpperCase);

		} else {
			String message = "Wrong fasta format for the file " + getFileName()
					+ "!\n The format should be: >name cellType vaccinationYear vaccinationDays \n ";
				message +="dna sequence"
						+ "\ni.e.: >FI1013VH B post09 d19 \n"
						+"GTGCAGTCTGGGGCTGAGGTGAGGAAGCCTGGGTCCTCGGTGAGGGTCTCCTGCAAG";
			throw new FastaFormatException(message);
		}
		scanner.close();
	}

	public HashMap<String,String> getFastaIdToSequence() {
		return fastaIdToSequence;
	}

	private String getFilePath() {
		return filePath;
	}

	private String getFileName() {
		File file = new File(filePath);
		return file.getName();
	}

	@SuppressWarnings("resource")
	private boolean areNumbersForCDROk(String fastaId) {
		Scanner scanner = new Scanner(fastaId);
		if (scanner.findInLine("\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s*$") != null) {
			MatchResult result = scanner.match();
			String numbers = result.group();
			String[] splitted = numbers.split("\\s+");
			int start = 0;
			for (String numb : splitted) {
				int nu = Integer.parseInt(numb);
				if (nu > start)
					start = nu;
				else
					return false;
			}
			return true;
		} else{
			return false;
		}
	}

	private boolean isImgtFormatOK(String sequence) {
		if (sequence.matches("\\.*[acgtACGT]+\\.+[acgtACGT]+.*")) {
			return true;
		} 
		return false;
	}

	private boolean isFastaIdOk(String fastaId) {
		if (!fastaId.matches("\\w+\\s+(B|PC)\\s+(post(\\d+)|pre(\\d+))\\s+(d(\\d+))?.*"))
			return false;
		else
			return true;
	}

	private boolean areDeletionsOK(String sequence) {
		// logger.debug("Processing deletions for sequence "+sequence);
		char[] nuc = sequence.toCharArray();
		int numberOfDeletion = 0;
		boolean previousOneIsDeletion = false;
		for (int i = 0; i < nuc.length; i++) {
			if (Character.toString(nuc[i]).equals("-")) {
				if ((!previousOneIsDeletion && numberOfDeletion == 0) || previousOneIsDeletion) {
					numberOfDeletion += 1;
				}
				previousOneIsDeletion = true;
			} else if (previousOneIsDeletion) {
				logger.debug("!!!!!!!We have " + numberOfDeletion + " deletions ");
				if (numberOfDeletion % 3 != 0)
					return false;
				previousOneIsDeletion = false;
				numberOfDeletion = 0;
			} else {
				previousOneIsDeletion = false;
			}
		}
		if (previousOneIsDeletion) {// in case the last nuc is a deletion!
			logger.debug("!!!!!!!We have " + numberOfDeletion + " deletions ");
			if (numberOfDeletion % 3 != 0)
				return false;
		}
		return true;
	}

	private boolean areTheDeletionsInTheFrame(String sequence) {
		char[] nuc = sequence.toCharArray();
		boolean previousOneIsDeletion = false;
		for (int i = 0; i < nuc.length; i++) {
			if (Character.toString(nuc[i]).equals("-")) {
				if (i % 3 != 0 && !previousOneIsDeletion) {
					if (isDNA)
						logger.warn("Wrong position for deletion at position (+1 ): " + i);
					return false;
				} else {
					previousOneIsDeletion = true;
				}
			} else {
				previousOneIsDeletion = false;
			}
		}
		return true;
	}

	private boolean isThereAStopCodon(String sequence){
		Translator translator = new Translator(sequence,isDNA);
		String protein = translator.getProteinSequence();
		if (protein.matches(".*X.*")){
			return true;
		}
		return false;
	}
	
	private boolean isTheNumberOfNucleotidesAMultipleOf3(String sequence) {
		if (sequence.length() % 3 != 0)
			return false;
		return true;
	}

	private void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}
