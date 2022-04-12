package com.example.grpc.client.grpcclient;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.grpc.client.grpcclient.MatrixRequest;
import com.example.grpc.client.grpcclient.Matrix;
import com.example.grpc.client.grpcclient.MatrixResponse;
import com.example.grpc.client.grpcclient.MatrixServiceGrpc;
import com.example.grpc.client.grpcclient.MatrixServiceGrpc.MatrixServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

@Service
public class GRPCClientService {

	public static void main(String[] args) throws InterruptedException {
	}

	static int[][] multiplyMatrix(int A[][], int B[][], double deadline) {
		printMatrix(A);
		ArrayList<int[][]> blockA = splitInBlocks(A);
		System.out.println("Split block A succesfully");
		System.out.println(blockA.toString());
		printMatrix(B);
		ArrayList<int[][]> blockB = splitInBlocks(B);
		System.out.println("Split block B succesfully");
		System.out.println(blockA.toString());
		System.out.println("Getting result");
		ArrayList<MatrixResponse> blocks = getResult(blockA, blockB, deadline);
		System.out.println("Got result");
		System.out.println("Assembling matrix");
		int[][] result = assembleMatrix(blocks, A.length, A[0].length);
		System.out.println("Assembled matrix succesfully");
		printMatrix(result);
		return result;
	}

	static ArrayList<MatrixResponse> getResult(ArrayList<int[][]> blockA, ArrayList<int[][]> blockB, double deadline) {
		System.out.println("Starting to get result");
		ArrayList<MatrixResponse> blocks = new ArrayList<>();
		ArrayList<MatrixServiceBlockingStub> stubs = null;

		Matrix[][] A = create2DBlocks(blockA);
		Matrix[][] B = create2DBlocks(blockB);

		int serversNeeded = 1;
		int currentServer = 0;
		int length = A.length;
		// we create all the servers as described in the getServers() function, but
		// we only use what we need
		stubs = getServers();
		System.out.println("Running loop");
		System.out.println("A length: " + length);
		for (int i = 0; i < A.length; i++) {
			System.out.println("Inside first loop");
			for (int j = 0; j < A.length; j++) {
				System.out.println("Inside second loop");
				for (int k = 0; k < A.length; k++) {
					System.out.println("Inside third loop");
					System.out.println("Starting to input matrix");
					Matrix A1 = A[i][k];
					System.out.println("Matrix A1:");
					printMatrixObject(A1);
					Matrix A2 = B[k][j];
					System.out.println("Matrix A2:");
					printMatrixObject(A2);
					System.out.println("Input matrix succesfully");
					if (i == 0 && j == 0 && k == 0) {
						System.out.println("Getting deadline");
						serversNeeded = getDeadline(A1, A2, blocks, stubs.get(currentServer), (blockA.size() * blockA.size()),
								deadline);
						continue;
					}
					System.out.println("Multiplying blocks");
					MatrixResponse C = stubs.get(currentServer).multiplyBlock(requestFromMatrix(A1, A2));
					currentServer++;
					if (currentServer == serversNeeded) {
						currentServer = 0;
					}
					blocks.add(C);
				}
			}
		}
		System.out.println("Starting to add the blocks from the multiplication");
		// here we add the blocks from the multiplication, starting with the block
		// from server 0
		currentServer = 0;
		ArrayList<MatrixResponse> addBlocks = new ArrayList<>();
		MatrixResponse lastResponse = null;
		int rows = A.length * 2;
		int rowLength = rows / 2;
		int index = 1;
		for (int i = 0; i < blocks.size(); i += rowLength) {
			for (int j = i; j < rowLength * index; j += 2) {
				if (j == i) {
					lastResponse = stubs.get(currentServer)
							.addBlock(requestFromBlockAddMatrix(blocks.get(j), blocks.get(j + 1)));
				} else {
					lastResponse = stubs.get(currentServer).addBlock(requestFromBlockAddMatrix(lastResponse, blocks.get(j)));
					j--;
				}
			}
			addBlocks.add(lastResponse);
			System.out.println("Added blocks from multiplication");
			index++;
			currentServer++;
			// once we reach the last server, we start from server 0 again
			if (currentServer == serversNeeded) {
				currentServer = 0;
			}
		}
		return addBlocks;
	}

	private static int[][] assembleMatrix(ArrayList<MatrixResponse> blocks, int rows, int columns) {
		int matrix[][] = new int[rows][columns];
		int index = 0;
		for (int i = 0; i < rows; i += 2) {
			for (int j = 0; j < columns; j += 2) {
				matrix[i][j] = blocks.get(index).getC().getC00();
				matrix[i][j + 1] = blocks.get(index).getC().getC01();
				matrix[i + 1][j] = blocks.get(index).getC().getC10();
				matrix[i + 1][j + 1] = blocks.get(index).getC().getC11();
				index++;
			}
		}
		return matrix;
	}

	static ArrayList<int[][]> splitInBlocks(int matrix[][]) {
		ArrayList<int[][]> tempArray = new ArrayList<>();

		int x = 2;
		int y = matrix.length;
		// here we loop through the first element of each column in the 2x2 block, and
		// we add 2 to i because we will move 2 columns to the right
		for (int i = 0; i < x - y + 1; i += 2) {
			// here we loop through the row of each block, and we add 2 to j because we will
			// move 2 positions down the row
			for (int j = 0; j < x - y + 1; j += 2) {
				boolean[][] assigned = new boolean[x][x];
				int[][] tempBlock = new int[x][x];
				// we fill the tempBlock 2x2 block with values from matrix
				for (int p = i; p < x + i; p++) {
					int step = 0;
					for (int q = j; q < x + j; q++) {
						if (step == 0 && !assigned[0][0]) {
							tempBlock[0][0] = matrix[p][q];
							assigned[0][0] = true;
							step++;
							continue;
						}
						if (step == 1 && !assigned[0][1]) {
							tempBlock[1][0] = matrix[p][q];
							assigned[0][1] = true;
							step++;
							continue;
						}
						if (step == 2 && !assigned[1][0]) {
							tempBlock[0][1] = matrix[p][q];
							assigned[1][0] = true;
							step++;
							continue;
						}
						if (step == 3 && !assigned[1][1]) {
							tempBlock[1][1] = matrix[p][q];
							assigned[1][1] = true;
							step++;
							continue;
						}
					}
				}
				System.out.println("Temporary Block:");
				printMatrix(tempBlock);
				tempArray.add(tempBlock); // add the 2x2 blocks to tempArray
			}
		}
		return tempArray;
	}

	public static ArrayList<MatrixServiceBlockingStub> getServers() {
		ManagedChannel[] channels = new ManagedChannel[8];
		ArrayList<MatrixServiceBlockingStub> stubs = new ArrayList<MatrixServiceBlockingStub>();

		String[] servers = new String[8];
		servers[0] = "10.128.0.22";
		servers[1] = "10.128.0.12";
		servers[2] = "10.128.0.13";
		servers[3] = "10.128.0.14";
		servers[4] = "10.128.0.15";
		servers[5] = "10.128.0.16";
		servers[6] = "10.128.0.17";
		servers[7] = "10.128.0.21";

		for (int i = 0; i < servers.length; i++) {
			channels[i] = ManagedChannelBuilder.forAddress(servers[i], 9090).usePlaintext().build();
			stubs.add(MatrixServiceGrpc.newBlockingStub(channels[i]));
		}
		return stubs;
	}

	public static Matrix makeBlockFromArray(int[][] array) {
		Matrix C = Matrix.newBuilder()
				.setC00(array[0][0])
				.setC01(array[0][1])
				.setC10(array[1][0])
				.setC11(array[1][1])
				.build();
		return C;
	}

	public static void printMatrixObject(Matrix matrix) {
		System.out.println("C00: " + matrix.getC00());
		System.out.println("C01: " + matrix.getC01());
		System.out.println("C10: " + matrix.getC10());
		System.out.println("C11: " + matrix.getC11());
	}

	static Matrix[][] create2DBlocks(ArrayList<int[][]> block) {
		System.out.println(block.toString());
		int sqr = (int) (Math.sqrt(Double.parseDouble("" + block.size())));
		System.out.println("SQR IN 2DBLOCKS IS: " + sqr);
		Matrix C[][] = new Matrix[sqr][sqr];
		int index = 0;
		for (int i = 0; i < sqr; i++) {
			System.out.println("Entered first for loop 2DBlocks");
			System.out.println(Arrays.deepToString(block.get(i)));
			Arrays.deepToString(block.get(i));
			for (int j = 0; j < sqr; j++) {
				System.out.println("Entered second for loop 2DBlocks");
				C[i][j] = makeBlockFromArray(block.get(index));
				printMatrixObject(C[i][j]);
				index++;
			}
		}
		return C;
	}

	public static MatrixRequest requestFromMatrix(Matrix matrix1, Matrix matrix2) {
		MatrixRequest request = MatrixRequest.newBuilder().setA(matrix1).setB(matrix2).build();
		return request;
	}

	public static MatrixRequest requestFromBlockAddMatrix(MatrixResponse matrix1, MatrixResponse matrix2) {
		MatrixRequest request = MatrixRequest.newBuilder().setA(matrix1.getC()).setB(matrix2.getC()).build();
		return request;
	}

	private static void printMatrix(int[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println("");
		}
		return;
	}

	static int getDeadline(Matrix A1, Matrix A2, ArrayList<MatrixResponse> responses, MatrixServiceBlockingStub stub,
			int numberOfBlocks, double deadline) {
		System.out.println("Matrix A1:");
		printMatrixObject(A1);
		System.out.println("Matrix A2:");
		printMatrixObject(A2);
		System.out.println("Starting to get deadline");
		int deadlineMilis = (int) (deadline * 1000);
		double startTime = System.currentTimeMillis();
		System.out.println("Start time: " + startTime + ", running multiplyBlock");
		MatrixResponse temp = stub.multiplyBlock(requestFromMatrix(A1, A2));
		responses.add(temp);
		System.out.println("Ran multiplyBlock sucessfully");
		double endTime = System.currentTimeMillis();
		double footprint = endTime - startTime;
		System.out.println("Footprint is " + footprint);
		double totalTime = (numberOfBlocks - 1) * footprint;
		double newDeadline = deadlineMilis - footprint;
		int serversNeeded = (int) (totalTime / newDeadline);

		System.out.println("Elapsed time for 1 block: " + footprint);
		System.out.println("Total elapsed time: " + totalTime);
		System.out.println("Number of blocks: " + numberOfBlocks);

		if (serversNeeded > 8) {
			serversNeeded = 8;
			System.out.println("Number of needed servers exceeds 8, setting to maximum of 8 servers");
			System.out.println();
		} else if (serversNeeded <= 1) {
			serversNeeded = 1;
			System.out.println("Number of needed servers is less than 1, setting to 1 server");
			System.out.println();
		}
		System.out.println("The number of needed servers is " + serversNeeded);
		return serversNeeded;
	}

}
