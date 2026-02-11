cmake_minimum_required(VERSION 3.16)

# Project Name
project(TicTacToe LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTOUIC ON)

# Find Qt6 components
find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets)

# Source files
set(SOURCES
    main.cpp
    mainwindow.cpp
    mainwindow.h
)

# Add executable
add_executable(TicTacToe_QT ${SOURCES})

# Link libraries
target_link_libraries(TicTacToe_QT PRIVATE Qt6::Core Qt6::Gui Qt6::Widgets)

# Handle Windows subsystem (hide console for release)
if(WIN32)
    set_property(TARGET TicTacToe_QT PROPERTY POPULATE_WIN32_SUBSYSTEM 1)
endif()
