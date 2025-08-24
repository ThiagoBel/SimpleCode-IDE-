#ifndef TASIL_H
#define TASIL_H

#include <string>

std::string print(const std::string& texto);
std::string printend(const std::string& textoend);
std::string println(const std::string& textoln);
std::string HelloWorld(const std::string& print);
std::string nfile(const std::string& name);
std::string nfilec(const std::string& namee, const std::string& content);
std::string cmd(const std::string& cmdd);
std::string ps(const std::string& cmddd);
std::string autorun(const std::string& execcmd);
std::string autorunnb(const std::string& execccmd);
std::string versionCM(const std::string& chavezinha);
std::string helpCM(const std::string& chavezinhaa);
std::string ProcessTime();
std::string os();
std::string random(const int& min, const int& max);
std::string readfile(const std::string& filename);
std::string del(const std::string& nome);
std::string repeat(const std::string& texto, int vezes);
std::string wait(double seconds);
std::string datetime();
std::string upper(const std::string& texto);
std::string lower(const std::string& texto);
std::string reverse(const std::string& texto);
std::string cls();
std::string getUsername();
std::string tasilVersion();
std::string backpack_put(const std::string& idt, const std::string& text);
std::string backpack_open(const std::string& ido);
std::string backpack_remove(const std::string& idr);

bool tf(const bool& valor);
bool testTasil(const bool& soun);
bool havefile(const std::string& filename);
bool contains(const std::string& texto, const std::string& sub);

template<typename T>
void debug(const std::string& label, T valor);

#endif
