/*
 * This file is part of java-psd-library.
 * 
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package psd.base;

import java.io.*;
import java.util.*;

import psd.layer.PsdLayer;
import psd.metadata.*;
import psd.parser.*;

public class PsdImage {
	private int numberOfChannels;
	private int width;
	private int height;
	private int depth;
	private PsdColorMode colorMode;
	private ArrayList<PsdLayer> layers;
	private PsdLayer baseLayer;

	private PsdAnimation animation;

	public PsdImage(File psdFile) throws IOException {
		Parser parser = new Parser();
		parser.setPsdHandler(new PsdHandler() {
			
			@Override
			public void headerLoaded(PsdHeader header) {
				numberOfChannels = header.getChannelsCount();
				width = header.getWidth();
				height = header.getHeight();
				depth = header.getDepth();
				colorMode = header.getColorMode();
			}

			@Override
			public void setAnimation(PsdAnimation animation) {
				PsdImage.this.animation = animation;
			}

			@Override
			public void setLayers(List<PsdLayer> layers) {
				PsdImage.this.layers = new ArrayList<PsdLayer>(layers);
				
			}

			@Override
			public void setBaseLayer(PsdLayer baseLayer) {
				PsdImage.this.baseLayer = baseLayer;
			}
		});
		
		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(psdFile));
		parser.parse(stream);
		stream.close();
		
		PsdLayer parentLayer = null;
		for (int i = getLayers().size() - 1; i >= 0; i--) {
			PsdLayer layer = getLayer(i);

			switch (layer.getType()) {
			case NORMAL:
				layer.setParent(parentLayer);
				break;
			case FOLDER:
				layer.setParent(parentLayer);
				parentLayer = layer;
				break;
			case HIDDEN:
				if (parentLayer != null) {
					parentLayer = parentLayer.getParent();
				}
				break;
			}
		}
	}
	
	public List<PsdLayer> getLayers() {
		if(this.layers == null){
			this.layers = new ArrayList<PsdLayer>();
			layers.add(this.baseLayer);
		}
		return Collections.unmodifiableList(layers);
	}
	
	public PsdLayer getLayer(int index) {
		return layers.get(index);
	}

	public PsdAnimation getAnimation() {
		return animation;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public PsdColorMode getColorMode() {
		return colorMode;
	}

	public int getDepth() {
		return depth;
	}

	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	public PsdLayer getBaseLayer() {
		return baseLayer;
	}

}
