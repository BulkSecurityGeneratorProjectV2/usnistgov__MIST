// ================================================================
//
// Disclaimer: IMPORTANT: This software was developed at the National
// Institute of Standards and Technology by employees of the Federal
// Government in the course of their official duties. Pursuant to
// title 17 Section 105 of the United States Code this software is not
// subject to copyright protection and is in the public domain. This
// is an experimental system. NIST assumes no responsibility
// whatsoever for its use by other parties, and makes no guarantees,
// expressed or implied, about its quality, reliability, or any other
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 4:18:09 PM EST
//
// Time-stamp: <Aug 1, 2013 4:18:09 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.fftw;

import gov.nist.isg.mist.stitching.lib.imagetile.ImageTile;
import gov.nist.isg.mist.stitching.lib32.imagetile.Stitching32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FFTW3Library32;
import gov.nist.isg.mist.stitching.lib32.imagetile.fftw.FftwImageTile32;
import gov.nist.isg.mist.stitching.lib.log.Log;
import gov.nist.isg.mist.stitching.lib.log.Log.LogType;
import gov.nist.isg.mist.stitching.lib.parallel.cpu.CPUStitchingThreadExecutor;
import gov.nist.isg.mist.stitching.lib.tilegrid.TileGrid;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.SequentialTileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridDirection;
import gov.nist.isg.mist.stitching.lib.tilegrid.loader.TileGridLoader.GridOrigin;
import gov.nist.isg.mist.timing.TimeUtil;

import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;

/**
 * Test case for stitching a grid of tiles with multithreading using FFTW.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TestFFTWGridPhaseCorrelationMultiThreaded32 {

  /**
   * Computes the phase correlation using a multiple thread on a grid of tiles using FFTW
   */
  public static void runTestGridPhaseCorrelation() throws FileNotFoundException {
    // UtilFnsStitching.disableUtilFnsNativeLibrary();
    Log.setLogLevel(LogType.INFO);
    int startRow = 0;
    int startCol = 0;
    int extentWidth = 23;
    int extentHeight = 30;

    Log.msg(LogType.MANDATORY, "Running Test Grid Phase Correlation Multithreaded FFTW");

    File tileDir = new File("C:\\majurski\\image-data\\1h_Wet_10Perc\\");
    FftwImageTile32.initLibrary("C:\\majurski\\NISTGithub\\MIST\\lib\\fftw", "", "libfftw3f");

    Log.msg(LogType.INFO, "Generating tile grid");
    TileGrid<ImageTile<Pointer<Float>>> grid = null;
    try {
      TileGridLoader loader =
          new SequentialTileGridLoader(23, 30, 1, "KB_2012_04_13_1hWet_10Perc_IR_0{pppp}.tif", GridOrigin.UR,
              GridDirection.VERTICALCOMBING);


      grid =
          new TileGrid<ImageTile<Pointer<Float>>>(startRow, startCol, extentWidth, extentHeight,
              loader, tileDir, FftwImageTile32.class);
    } catch (InvalidClassException e) {
      Log.msg(LogType.MANDATORY, e.getMessage());
    }

    if (grid == null)
      return;

    ImageTile<Pointer<Float>> tile = grid.getSubGridTile(0, 0);
    tile.readTile();

    Log.msg(LogType.INFO, "Loading FFTW plan");

    FftwImageTile32.initPlans(tile.getWidth(), tile.getHeight(), FFTW3Library32.FFTW_MEASURE, true, "test.dat");

    FftwImageTile32.savePlan("test.dat");


    int numProducers = 1;
    int numWorkers = 22;
    CPUStitchingThreadExecutor<Pointer<Float>> executor =
        new CPUStitchingThreadExecutor<Pointer<Float>>(numProducers, numWorkers, tile, grid);

    tile.releasePixels();

    Log.msg(LogType.INFO, "Computing translations");
    TimeUtil.tick();
    executor.execute();

    Log.msg(LogType.INFO, "Computing global optimization");


    Stitching32.outputRelativeDisplacements(grid, new File(
        "C:\\majurski\\image-data\\1h_Wet_10Perc\\fftw",
        "relDisp.txt"));

    Log.msg(LogType.MANDATORY, "Completed Test in " + TimeUtil.tock() + " ms");


//    Stitching32.printRelativeDisplacements(grid);

  }

  /**
   * Executes the test case
   *
   * @param args not used
   */
  public static void main(String args[]) {
    try {
      TestFFTWGridPhaseCorrelationMultiThreaded32.runTestGridPhaseCorrelation();
    } catch (FileNotFoundException e) {
      Log.msg(LogType.MANDATORY, "Unable to find file: " + e.getMessage());
    }
  }
}
