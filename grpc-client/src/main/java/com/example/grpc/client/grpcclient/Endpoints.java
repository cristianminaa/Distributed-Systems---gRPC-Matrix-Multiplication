package com.example.grpc.client.grpcclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.grpc.client.grpcclient.storage.StorageFileNotFoundException;
import com.example.grpc.client.grpcclient.storage.StorageService;

@RestController
public class Endpoints {

	private final StorageService storageService;
	GRPCClientService grpcClientService;
	int[][] matrix1;
	int[][] matrix2;
	double deadline;
	boolean firstMatrixUploaded = false;
	boolean secondMatrixUploaded = false;

	@Autowired
	public Endpoints(GRPCClientService grpcClientService, StorageService storageService) {
		this.storageService = storageService;
		this.grpcClientService = grpcClientService;
	}

	@GetMapping("/main")
	public String showUploadedFiles(Model model) throws IOException {
		model.addAttribute("files",
				storageService.loadAll()
						.map(path -> MvcUriComponentsBuilder
								.fromMethodName(Endpoints.class, "serveFile", path.getFileName().toString())
								.build().toUri().toString())
						.collect(Collectors.toList()));
		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> fileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

	@PostMapping("/uploadDeadline")
	public void deadlineUpload(@RequestParam("deadline") double uploadedDeadline) {
		deadline = uploadedDeadline;
		System.out.println("Uploaded deadline is " + deadline);
	}

	@PostMapping("/upload")
	public void fileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		System.out.println(file);
		if (!file.isEmpty()) {
			try {
				// we first check if the first matrix was uploaded, then
				// we try to convert it to an array, and if it is null
				// the program throws an error message
				if (!firstMatrixUploaded) {
					matrix1 = arrayFromFile(file);
					if (matrix1 == null) {
						redirectAttributes.addFlashAttribute("message", "Invalid matrix file " + file.getOriginalFilename() + "!");
					}
					// if matrix is successfully converted to an array, then we check
					// for any errors, and if there are, the program throws an error
					// otherwise if all good, save
					else {
						int rows = matrix1.length;
						int columns = matrix1[0].length;
						if (rows < 1 || columns < 1 || rows != columns || isPowerOfTwo(rows) == false
								|| isPowerOfTwo(columns) == false) {
							redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
							throw new Exception(
									"Invalid upload! Check your matrix. Make sure it is a square matrix that is a power of 2.");
						} else {
							System.out.println("First matrix succesfully uploaded");
							printMatrix(matrix1);
							firstMatrixUploaded = true;
							storageService.store(file);
						}
					}
				} else if (!secondMatrixUploaded) {
					matrix2 = arrayFromFile(file);
					if (matrix2 == null) {
						redirectAttributes.addFlashAttribute("message",
								"Invalid Matrix! Check error message for file " + file.getOriginalFilename() + "!");
					} else {
						int rows = matrix2.length;
						int columns = matrix2[0].length;
						if (rows < 1 || columns < 1 || rows != columns || isPowerOfTwo(rows) == false
								|| isPowerOfTwo(columns) == false) {
							redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
							throw new Exception(
									"Invalid upload! Check your matrix. Make sure it is a square matrix that is a power of 2.");
						} else {
							System.out.println("Second matrix succesfully uploaded");
							printMatrix(matrix2);
							secondMatrixUploaded = true;
							storageService.store(file);
						}
					}
				}
				// we are catching exceptions here in case of any unforeseen errors
			} catch (Exception e) {
				redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
				System.out.println("Invalid upload! Check your matrix. Make sure it is a square matrix that is a power of 2.");
			}
		}
		if (firstMatrixUploaded || secondMatrixUploaded) {
			redirectAttributes.addFlashAttribute("message",
					"You successfully uploaded the file " + file.getOriginalFilename() + " containing a matrix!");
			System.out.println("You successfully uploaded the file " + file.getOriginalFilename() + " containing a matrix!");
		}
		if (firstMatrixUploaded && secondMatrixUploaded) {
			System.out
					.println("You successfully uploaded both files/matrices");
		}
	}

	@GetMapping("/simpleMult")
	public String simpleMult(HttpServletRequest request, Model uiModel, RedirectAttributes redirectAttributes) {
		if (firstMatrixUploaded && secondMatrixUploaded) {
			int[][] result = GRPCClientService.multiplyMatrix(matrix1, matrix2, Double.MAX_VALUE);
			// System.out.println("Simple multiplication");
			redirectAttributes.addAttribute("result is ", result);
			// System.out.println("result is " + result);
			return "result is " + result;
		} else {
			redirectAttributes.addFlashAttribute("message",
					"You need to upload both  matrices first, and they need to be valid (i.e. a power of 2)!");
			return "Error message: You need to upload both  matrices first, and they need to be valid (i.e. a power of 2)!";
		}
	}

	@GetMapping("/deadlineMult")
	public String deadlineMult(HttpServletRequest request, Model uiModel, RedirectAttributes redirectAttributes) {
		if (firstMatrixUploaded && secondMatrixUploaded) {
			int[][] result = GRPCClientService.multiplyMatrix(matrix1, matrix2, deadline);
			// System.out.println("Simple multiplication");
			redirectAttributes.addAttribute("Result is ", result);
			// System.out.println("result is " + result);
			return "result is " + result;
		} else {
			redirectAttributes.addFlashAttribute("message",
					"You need to upload both  matrices first, and they need to be valid (i.e. a power of 2)!");
			return "Error message: You need to upload both  matrices first, and they need to be valid (i.e. a power of 2)!";
		}
	}

	public static int[][] arrayFromFile(MultipartFile file) {
		ArrayList<int[]> cells = new ArrayList<int[]>();
		try {
			byte[] bytes;
			String data;
			String[] dataLines;
			bytes = file.getBytes();
			data = new String(bytes);
			dataLines = data.split("\n");
			for (String line : dataLines) {
				String[] numberAsString = line.split(" ");
				int[] numbers = new int[numberAsString.length];
				for (int i = 0; i < numberAsString.length; i++) {
					numbers[i] = (int) Integer.valueOf(numberAsString[i]);
				}
				cells.add(numbers);
			}
		} catch (Exception e) {
			System.out.println("Error converting the file to an array!");
			System.out.println(e.toString());
			return null;
		}
		int[][] cellsArray = MatrixAsArrayFromArrayList(cells);
		return cellsArray;
	}

	public static int[][] MatrixAsArrayFromArrayList(ArrayList<int[]> array) {
		int rows = array.size();
		int cols = array.get(0).length;
		int result[][] = new int[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				result[i][j] = array.get(i)[j];
			}
		}
		return result;
	}

	static boolean isPowerOfTwo(int n) { // https://www.geeksforgeeks.org/java-program-to-find-whether-a-no-is-power-of-two/
		if (n == 0)
			return false;

		while (n != 1) {
			if (n % 2 != 0)
				return false;
			n = n / 2;
		}
		return true;
	}

	private static void printMatrix(int[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}
		return;
	}
}
