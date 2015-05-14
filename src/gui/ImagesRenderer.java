package gui;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

public class ImagesRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = -1710983607575867230L;
	Font font = new Font("helvitica", Font.BOLD, 24);
	Map<String, ImageIcon> imageMap;
	
	public ImagesRenderer(Map map) {
		imageMap = map;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
				index, isSelected, cellHasFocus);
		label.setIcon(imageMap.get((String) value));
		label.setHorizontalTextPosition(JLabel.RIGHT);
		label.setFont(font);
		return label;
	}
}
