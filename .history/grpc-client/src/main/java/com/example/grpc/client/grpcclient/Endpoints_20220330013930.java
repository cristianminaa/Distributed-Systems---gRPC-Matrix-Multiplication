package com.example.grpc.client.grpcclient;

import com.example.grpc.server.Request;
import com.example.grpc.server.MatrixResponse;
import com.example.grpc.server.Matrix;
import com.example.grpc.server.MatrixServiceGrpc.MatrixServiceImplBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

import java.io.IOException;

@Controller
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

	@GetMapping("/")
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

	@PostMapping("/")
	public String fileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
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
						return "redirect:/";
					}
					// if matrix is successfully converted to an array, then we check
					// for any errors, and if there are, the program throws an error
					// otherwise if all good, save
					else {
						int rows = matrix1.length;
						int columns = matrix1[0].length;
						if (rows < 1 || columns < 1 || rows != columns || !isPowerOfTwo(rows) || !isPowerOfTwo(columns)) {
							redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
							return "redirect:/";
						} else {
							firstMatrixUploaded = true;
							storageService.store(file);
						}
					}
				} else if (!secondMatrixUploaded) {
					matrix2 = arrayFromFile(file);
					if (matrix2 == null) {
						redirectAttributes.addFlashAttribute("message",
								"Invalid Matrix! Check error message for file " + file.getOriginalFilename() + "!");
						return "redirect:/";
					} else {
						int rows = matrix2.length;
						int columns = matrix2[0].length;
						if (rows < 1 || columns < 1 || rows != columns || !isPowerOfTwo(rows) || !isPowerOfTwo(columns)
								|| rows != matrix2.length || columns != matrix2[0].length) {
							redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
							return "redirect:/";
						} else {
							secondMatrixUploaded = true;
							storageService.store(file);
						}
					}
				}
				// we are catching exceptions here in case of any unforeseen errors
			} catch (Exception e) {
				redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
				return "redirect:/";
			}
		}
		if (firstMatrixUploaded || secondMatrixUploaded) {
			redirectAttributes.addFlashAttribute("message",
					"You successfully uploaded the file " + file.getOriginalFilename() + " containing 2 matrices!");
			return "redirect:/";
		}
		redirectAttributes.addFlashAttribute("message", "Invalid upload! Check your matrix.");
		return "redirect:/";
	}

	@RequestMapping(value = "/", method = RequestMethod.POST, params = "simpleMult")
	public String simpleMult(HttpServletRequest request, Model uiModel, RedirectAttributes redirectAttributes) {
		int[][] result = grpcClientService.multiplyMatrix(matrix1, matrix2, Double.MAX_VALUE);
		System.out.println("Simple multiplication");
		redirectAttributes.addAttribute("result", result);
		return "redirect:/result/{result}";
	}

	@RequestMapping(value = "/", method = RequestMethod.POST, params = "deadlineMult")
	public String deadlineMult(@RequestParam("deadline") String seconds, HttpServletRequest request, Model uiModel,
			RedirectAttributes redirectAttributes) {
		Double secondsDouble = Double.valueOf(seconds);
		int[][] result = grpcClientService.multiplyMatrix(matrix1, matrix2, secondsDouble);
		redirectAttributes.addAttribute("result", result);
		System.out.println(seconds);
		System.out.println("Deadline multiplication");
		return "redirect:/result/{result}";
	}

	public static int[][] arrayFromFile(MultipartFile file) {
		ArrayList<int[]> cells = new ArrayList<int[]>();
		try {
			byte[] bytes;
			String data;
			String[] dataLines;
			bytes = file.getBytes();
			data = new String(bytes);
			dataLines = data.split("\r\n");
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

	private static boolean isPowerOfTwo(int number) {
		try {
			if (number > 0 && ((number & (number - 1)) == 0)) {
				return number > 0 && ((number & (number - 1)) == 0);
			}
		} catch (Exception e) {
			System.out.println("The matrix size is not a power of two!");
			return false;
		}
		return number > 0 && ((number & (number - 1)) == 0);
	}
}
