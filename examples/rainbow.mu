module testing {

	mut? r = 255;
	mut? g = 0;
	mut? b = 0;
	
	pub? moveCursorHome = "\u001b[H";
	
	pub enum Hi {
		Hello, Test
	}
	
	pub fn rainbow {
		if (r == 255 && g < 255 && b == 0) {
			g += 5;
		} else if (r > 0 && g == 255 && b == 0) {
			r -= 5;
		} else if (r == 0 && g == 255 && b < 255) {
			b += 5;
		} else if (r == 0 && g > 0 && b == 255) {
			g -= 5;
		} else if (r < 255 && g == 0 && b == 255) {
			r += 5;
		} else if (r == 255 && g == 0 && b > 0) {
			b -= 5;
		}
	}
	
	pub fn setCursorVisible~ (bool v) {
		if v {
			std.print("\u001b[?25h");
			ret;
		}
		std.print("\u001b[?25l");
	}
	
	pub fn executeRainbow~ (long wait_long, string text) {
		use std;
		use termion;
				
		setCursorVisible(false);
		
		while true {
			print(moveCursorHome);
			println(color_fg(r, g, b) + text);
			rainbow();
			sleep(wait_long);
		}
	}
	
	pub fn main~ (string wait, string text) {
		long wait_long = long(wait);
		executeRainbow(wait_long, text);
	}
	
	pub fn main~ (string wait) {
		main(wait, "µ");
	}
	
	pub fn main {
		main("10", "µ");
	}
	
}
