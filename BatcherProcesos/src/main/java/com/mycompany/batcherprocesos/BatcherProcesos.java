/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.batcherprocesos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import model.Job;
import model.SchedulerPolicy;
import service.Batcher;
import service.YAMLReader;

/**
 *
 * @author carlos
 */
public class BatcherProcesos {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            Path jobsPath = Path.of(getValue(args, 0, scanner, "Carpeta de jobs [jobs]: ", "jobs"));
            int cores = Integer.parseInt(getValue(args, 1, scanner, "Cores simulados [4]: ", "4"));
            int memMb = Integer.parseInt(getValue(args, 2, scanner, "Memoria simulada en MB [2048]: ", "2048"));
            SchedulerPolicy policy = getPolicy(getValue(args, 3, scanner, "Politica FCFS/RR [FCFS]: ", "FCFS"));
            long quantum = Long.parseLong(getQuantum(args, scanner, policy));

            YAMLReader reader = new YAMLReader();
            List<Job> jobs = reader.loadJobs(jobsPath);
            if (jobs.isEmpty()) {
                System.out.println("No hay jobs validos para ejecutar en " + jobsPath.toAbsolutePath());
                return;
            }

            Batcher batcher = new Batcher(cores, memMb, policy, quantum);
            batcher.loadJobs(jobs);
            batcher.run();
        } catch (IOException e) {
            System.err.println("Error leyendo jobs: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Parametros no validos: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Ejecucion interrumpida");
        }
    }

    private static String ask(Scanner scanner, String message, String defaultValue) {
        System.out.print(message);
        String value = scanner.nextLine().trim();

        if (value.isEmpty()) {
            return defaultValue;
        } else {
            return value;
        }
    }

    private static String getValue(String[] args, int index, Scanner scanner, String message, String defaultValue) {
        if (args.length > index) {
            return args[index];
        } else {
            return ask(scanner, message, defaultValue);
        }
    }

    private static String getQuantum(String[] args, Scanner scanner, SchedulerPolicy policy) {
        if (args.length > 4) {
            return args[4];
        }

        if (policy == SchedulerPolicy.RR) {
            return ask(scanner, "Quantum RR en ms [200]: ", "200");
        } else {
            return "200";
        }
    }

    private static SchedulerPolicy getPolicy(String text) {
        return SchedulerPolicy.valueOf(text.trim().toUpperCase());
    }
}
