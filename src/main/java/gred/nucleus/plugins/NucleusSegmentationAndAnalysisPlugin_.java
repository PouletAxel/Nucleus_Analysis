package gred.nucleus.plugins;
import gred.nucleus.core.*;
import gred.nucleus.dialogs.NucleusSegmentationAndAnalysisDialog;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.GaussianBlur3D;
import ij.plugin.PlugIn;

/**
 *  Method to segment and analyse the nucleus on one image
 *  
 * @author Poulet Axel
 *
 */
public class NucleusSegmentationAndAnalysisPlugin_ implements PlugIn{
	 /** image to process*/
	ImagePlus m_imgPlus;
	
	/**
	 * 
	 */
	public void run(String arg)
	{
		m_imgPlus = WindowManager.getCurrentImage();
		if(null == m_imgPlus){
			IJ.noImage();
			return;
		}
		else if(m_imgPlus.getStackSize() == 1 || (m_imgPlus.getType() != ImagePlus.GRAY8 && m_imgPlus.getType() != ImagePlus.GRAY16)){
			IJ.error("Image format", "No image in 8 or 16 bits gray scale  in 3D");
			return;
		}
		if(IJ.versionLessThan("1.32c"))
			return;
		NucleusSegmentationAndAnalysisDialog nuc = new NucleusSegmentationAndAnalysisDialog(m_imgPlus.getCalibration());
		while(nuc.isShowing()){
	    	 try {Thread.sleep(1);}
	    	 catch (InterruptedException e) {e.printStackTrace();}
	    }
	   
		if(nuc.isStart()){
			double xCalibration =nuc.getXCalibration();
			double yCalibration = nuc.getYCalibration();
			double zCalibration = nuc.getZCalibration();
			String unit = nuc.getUnit();
			double volumeMin = nuc.getMinVolume();
			double volumeMax = nuc.getMaxVolume();
			Calibration calibration = new Calibration();
			calibration.pixelDepth = zCalibration;
			calibration.pixelWidth = xCalibration;
			calibration.pixelHeight = yCalibration;
			calibration.setUnit(unit);
			m_imgPlus.setCalibration(calibration);
			GaussianBlur3D.blur(m_imgPlus,0.25,0.25,1);
			ImageStack imageStack= m_imgPlus.getStack();
			int max = 0;
			for(int k = 0; k < m_imgPlus.getStackSize(); ++k)
				for (int i = 0; i < m_imgPlus.getWidth(); ++i )
					for (int j = 0; j < m_imgPlus.getHeight(); ++j){
						if (max < imageStack.getVoxel(i, j, k)){
							max = (int) imageStack.getVoxel(i, j, k);
						}
					}
			IJ.setMinAndMax(m_imgPlus, 0, max);	
			IJ.run(m_imgPlus, "Apply LUT", "stack");
			IJ.log("Begin image processing "+m_imgPlus.getTitle());
			NucleusSegmentation nucleusSegmentation = new NucleusSegmentation();
			nucleusSegmentation.setVolumeRange(volumeMin, volumeMax);
			ImagePlus imagePlusSegmented = nucleusSegmentation.run(m_imgPlus);
			if (nucleusSegmentation.getBestThreshold() > 0){
				imagePlusSegmented.show();
				NucleusAnalysis nucleusAnalysis = new NucleusAnalysis(m_imgPlus);
				if (nuc.is2D3DAnalysis()){
					nucleusAnalysis.nucleusParameter3D(imagePlusSegmented);
					nucleusAnalysis.nucleusParameter2D(imagePlusSegmented);
				}
				else if(nuc.is3D())
					nucleusAnalysis.nucleusParameter3D(imagePlusSegmented);
				else
					nucleusAnalysis.nucleusParameter2D(imagePlusSegmented);
			}
		}
	}
}