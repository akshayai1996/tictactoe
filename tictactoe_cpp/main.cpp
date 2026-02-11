#include "mainwindow.h"
#include <QApplication>
#include <QStyleFactory>

int main(int argc, char *argv[]) {
  // Force High DPI scaling
  QApplication::setAttribute(Qt::AA_EnableHighDpiScaling);

  QApplication a(argc, argv);

  // Force Fusion style for consistent look across platforms
  a.setStyle(QStyleFactory::create("Fusion"));

  MainWindow w;
  w.show();
  return a.exec();
}
