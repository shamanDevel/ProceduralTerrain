/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.voronoi;

import com.jme3.math.Vector2f;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Sebastian Weiss
 */
public class DisplayVoronoiDiagram extends JFrame {
	private static final int w = 500;
	private static final int h = 500;
	private static final int scale = 20;
	private static final int n = 50;
	
	private JPanel panel;
	private List<Vector2f> points;
	private List<Edge> edges;

	public DisplayVoronoiDiagram() throws HeadlessException {
		setTitle("Voronoi Diagram");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				paintPanel(g);
			}
			
		};
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(w, h));
		pack();
		generateVoronoi();
	}
	
	private void generateVoronoi() {
		Random rand = new Random();
		Voronoi voronoi = new Voronoi();
		points = new ArrayList<>(n);
		for (int i=0; i<n; ++i) {
			float x = rand.nextFloat() * w * scale;
			float y = rand.nextFloat() * h * scale;
			points.add(new Vector2f(x, y));
		}
		edges = voronoi.getEdges(points, w*scale, h*scale);
		System.out.println("edges: ("+edges.size()+")");
		for (Edge e : edges) {
			System.out.println(" "+e.start+" -- "+e.end);
		}
		panel.repaint();
	}
	
	private void paintPanel(Graphics g) {
		Graphics2D G = (Graphics2D) g;
		G.setStroke(new BasicStroke(3));
		G.setPaint(Color.RED);
		for (Edge e : edges) {
			float x1 = e.start.x / scale;
			float y1 = e.start.y / scale;
			float x2 = e.end.x / scale;
			float y2 = e.end.y / scale;
			Line2D l = new Line2D.Float(x1, y1, x2, y2);
			G.draw(l);
		}
		G.setPaint(Color.BLACK);
		for (Vector2f p : points) {
			int x = (int) (p.x / scale);
			int y = (int) (p.y / scale);
			G.fillOval(x-1, y-1, 3, 3);
		}
	}
	
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		DisplayVoronoiDiagram f = new DisplayVoronoiDiagram();
		f.setVisible(true);
		f.setLocationRelativeTo(null);
	}
	
}
