/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

public class TestCam {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        VideoCapture cam = new VideoCapture(0);
        if (cam.isOpened()) {
            System.out.println("✅ Cámara abierta correctamente.");
        } else {
            System.out.println("❌ No se pudo abrir la cámara.");
        }
        cam.release();
    }
}